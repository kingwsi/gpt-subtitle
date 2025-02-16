package org.example.gptsubtitle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.gptsubtitle.common.Response;
import org.example.gptsubtitle.openai.MessageRole;
import org.example.gptsubtitle.openai.OpenaiClient;
import org.example.gptsubtitle.openai.OpenaiRequest;
import org.example.gptsubtitle.openai.OpenaiResult;
import org.example.gptsubtitle.subtitle.SRTParser;
import org.example.gptsubtitle.subtitle.Subtitle;
import org.example.gptsubtitle.subtitle.SubtitleInfo;
import org.example.gptsubtitle.subtitle.TranslateParam;
import org.example.gptsubtitle.subtitle.TranslateRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.example.gptsubtitle.config.AppConfig;

@Slf4j
@RestController
@RequestMapping("/api/subtitle")
public class SubtitleController {
    // 用于存储文件哈希值和ID的映射关系
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    private final ObjectMapper objectMapper;

    public SubtitleController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/translate")
    public Response<List<String>> translate(@RequestBody TranslateParam translateParam) throws IOException {
        String fileId = translateParam.getFileId();
        Integer begin = translateParam.getBegin();
        AppConfig config = translateParam.getConfig();
        List<Subtitle> subtitles = Optional.ofNullable(getSubtitleInfo(fileId))
                .map(Response::getData)
                .map(SubtitleInfo::getSubtitles)
                .orElseThrow(() -> new RuntimeException("Subtitle not found"));

        Path translateMapPath = Paths.get(TEMP_DIR, fileId + "_map.json");
        if (!Files.exists(translateMapPath)) {
            Files.createFile(translateMapPath);
        }
        Map<Integer, String> translatedMap = new HashMap<>();
        if (translateMapPath.toFile().length() > 0) {
            translatedMap = objectMapper.readValue(translateMapPath.toFile(), new TypeReference<>() {
            });
        }

        int lastTranslatedIndex = 0;
        if (begin != null) {
            if (begin > subtitles.size() || begin < 0) {
                throw new RuntimeException("Begin index out of range");
            }
            lastTranslatedIndex += begin;
        } else {
            begin = 0;
            for (Subtitle sub : subtitles) {
                String s = translatedMap.get(sub.getIndex());
                if (s == null || s.isBlank()) {
                    begin = sub.getIndex();
                    lastTranslatedIndex = sub.getIndex();
                    break;
                }
                sub.setTranslatedText(s);
                sub.setTranslated(true);
            }
        }
        int historyBegin = Math.max(begin - config.getChunkSize() * config.getMessageMaxSize(), 0);

        List<Subtitle> historySubtitles = subtitles.subList(historyBegin, begin);

        OpenaiClient openaiClient = new OpenaiClient(config.getBaseApi(), config.getAppKey());
        openaiClient.setMessageLimit(config.getMessageMaxSize());

        TranslateRequest translateRequest = new TranslateRequest();
        translateRequest.setConfig(config);
        
        historySubtitles = historySubtitles.stream()
                .filter(o -> StringUtils.hasText(o.getTranslatedText()))
                .collect(Collectors.toCollection(ArrayList::new));
        List<List<Subtitle>> histories = SRTParser.partitionList(historySubtitles, config.getChunkSize());
        for (List<Subtitle> subs : histories) {
            List<String> originalTexts = subs.stream().map(Subtitle::getText).collect(Collectors.toList());
            List<String> translatedTexts = subs.stream().map(Subtitle::getTranslatedText).collect(Collectors.toList());
            translateRequest.addMessage(new OpenaiRequest.Message(MessageRole.user, objectMapper.writeValueAsString(originalTexts)));
            translateRequest.addMessage(new OpenaiRequest.Message(MessageRole.assistant, objectMapper.writeValueAsString(translatedTexts)));
        }

        int endIndex = lastTranslatedIndex + config.getChunkSize();
        List<Subtitle> list = subtitles.subList(begin, Math.min(endIndex, subtitles.size()));
        if (list.isEmpty()) {
            throw new RemoteException("list is empty!");
        }
        List<String> chunkTexts = list.stream().map(Subtitle::getText).collect(Collectors.toCollection(ArrayList::new));

        translateRequest.setChunkSize(chunkTexts.size());
        translateRequest.addMessage(new OpenaiRequest.Message(MessageRole.user, objectMapper.writeValueAsString(chunkTexts)));
        String result = openaiClient.completions(translateRequest);
        OpenaiResult completions = objectMapper.readValue(result, OpenaiResult.class);
        if (completions == null || completions.getChoices() == null) {
            throw new RemoteException("Translate Response is empty!");
        }
        OpenaiRequest.Message message = completions.getChoices()
                .stream().findFirst()
                .map(OpenaiResult.Choice::getMessage).orElse(null);
        if (message == null || message.getContent() == null || message.getContent().isEmpty()) {
            throw new RemoteException("Translate Message is empty!");
        }
        String resultContent = message.getContent();
        List<String> translates;
        try {
            translates = objectMapper.readValue(resultContent, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("解析失败！" + resultContent);
        }
        log.info("assistant -> {}", IntStream.range(0, translates.size())
                .mapToObj(i -> (i + 1) + ". " + translates.get(i))
                .collect(Collectors.joining(", ")));
        if (chunkTexts.size() != translates.size()) {
            throw new RuntimeException("chunk text size mismatch");
        }
        // success
        for (int i = 0; i < list.size(); i++) {
            Subtitle subtitle = list.get(i);
            subtitle.setTranslated(true);
            translatedMap.put(subtitle.getIndex(), translates.get(i));
        }
        objectMapper.writeValue(translateMapPath.toFile(), translatedMap);
        log.info("Write to map file：{}", translateMapPath.toAbsolutePath());
        return Response.ok(translates);
    }

    @GetMapping("/getSubtitleInfo")
    public Response<SubtitleInfo> getSubtitleInfo(String fileId) throws IOException {
        Path uploadPath = Paths.get(TEMP_DIR, fileId);
        if (!Files.exists(uploadPath)) {
            throw new RuntimeException("File not found, You need to upload a file");
        }
        List<Subtitle> subtitleList = SRTParser.parseSRT(uploadPath);
        Path translateMapPath = Paths.get(TEMP_DIR, fileId + "_map.json");
        if (!Files.exists(translateMapPath)) {
            Files.createFile(translateMapPath);
        }

        Map<Integer, String> translateMap = new HashMap<>();
        if (translateMapPath.toFile().length() > 0) {
            translateMap = objectMapper.readValue(translateMapPath.toFile(), new TypeReference<>() {
            });
        }

        for (int i = 0; i < subtitleList.size(); i++) {
            Subtitle subtitle = subtitleList.get(i);
            String translateText = translateMap.get(subtitle.getIndex());
            subtitle.setRealIndex(i);
            if (StringUtils.hasText(translateText)) {
                subtitle.setTranslatedText(translateText);
                subtitle.setTranslated(true);
            }
        }
        return Response.ok(new SubtitleInfo(fileId, subtitleList));
    }

    @PostMapping("/upload")
    public Response<String> uploadFile(@RequestParam("file") MultipartFile file) {
        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".srt")) {
            throw new RuntimeException("Invalid file format, Only .srt files are allowed");
        }

        try {
            // Calculate file's MD5 hash
            String fileHash = calculateMD5(file.getInputStream());

            // Get system temporary directory
            String tmpDir = System.getProperty("java.io.tmpdir");
            Path uploadPath = Paths.get(tmpDir, fileHash);

            // Check if file already exists
            if (Files.exists(uploadPath)) {
                return Response.ok(fileHash);
            }

            // Save file to temporary directory, using hash as file name
            Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);

            return Response.ok(fileHash);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    private String calculateMD5(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, length);
        }
        byte[] digest = md.digest();

        // Use HexFormat (Java 17+)
        return HexFormat.of().formatHex(digest);
    }

    @GetMapping("/getConfig")
    public Response<AppConfig> getConfig() {
        return Response.ok(new AppConfig());
    }
}
