package org.example.gptsubtitle.subtitle;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
/**
 * description:  <br>
 * date: 2025/2/9 15:14 <br>
 */
@Slf4j
public class SRTParser {
    public static List<Subtitle> parseSRT(Path filePath) throws IOException {
        List<Subtitle> subtitles = new ArrayList<>();
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        int index = 0;
        String timestamp = "";
        StringBuilder textBuilder = new StringBuilder();
        boolean isText = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                if (index != 0 && !timestamp.isEmpty() && !textBuilder.isEmpty()) {
                    subtitles.add(new Subtitle(index, timestamp, textBuilder.toString().trim()));
                    textBuilder.setLength(0);
                }
                isText = false;
            } else if (!isText) {
                try {
                    index = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    log.error("Error parsing SRT file: {}", line);
                }
                isText = true;
            } else if (line.contains("-->")) {
                timestamp = line;
            } else {
                textBuilder.append(line).append(" ");
            }
        }

        if (index != 0 && !timestamp.isEmpty() && !textBuilder.isEmpty()) {
            subtitles.add(new Subtitle(index, timestamp, textBuilder.toString().trim()));
        }
        return subtitles;
    }

    public static void writeSRT(String filePath, List<Subtitle> subtitles) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Subtitle subtitle : subtitles) {
                writer.write(subtitle.getIndex() + "\n");
                writer.write(subtitle.getTimestamp() + "\n");
                writer.write(subtitle.getText() + "\n\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> partitionedList = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            partitionedList.add(new ArrayList<>(list.subList(i, Math.min(i + chunkSize, list.size()))));
        }
        return partitionedList;
    }
}
