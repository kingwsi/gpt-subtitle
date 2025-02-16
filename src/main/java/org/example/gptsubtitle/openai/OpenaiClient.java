package org.example.gptsubtitle.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.gptsubtitle.config.AppConfig;
import org.example.gptsubtitle.subtitle.TranslateRequest;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * description:  <br>
 * date: 2025/2/13 20:46 <br>
 */
@Slf4j
@Data
public class OpenaiClient {

    private String baseApi;
    private String apiKey;
    
    private Integer messageLimit = 10;

    public OpenaiClient(String baseApi, String apiKey) {
        this.apiKey = apiKey;
        this.baseApi = baseApi;
    }

    public String completions(TranslateRequest translateRequest) throws IOException {
        // 创建 OkHttpClient 实例
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS) // 连接超时
                .readTimeout(30, TimeUnit.SECONDS)     // 读取超时
                .writeTimeout(30, TimeUnit.SECONDS)    // 写入超时
                .callTimeout(60, TimeUnit.SECONDS)     // 整个请求超时
                .build();
        
        if (translateRequest.getMessages() == null || translateRequest.getMessages().isEmpty()) {
            throw new IllegalArgumentException("messages cannot be null or empty");
        }

        AppConfig config = translateRequest.getConfig();

        OpenaiRequest openaiRequest = new OpenaiRequest();
        openaiRequest.setModel(config.getModel());
        openaiRequest.setStream(false);
        openaiRequest.setMaxTokens(config.getMaxTokens());
        
        List<OpenaiRequest.Message> messages = new ArrayList<>();

        // system
        if (!StringUtils.hasText(config.getPrompt())) {
            messages.add(new OpenaiRequest.Message(MessageRole.system, config.getPrompt()));
        }

        // JsonSchema
        if (config.isEnableJsonSchema() && translateRequest.getChunkSize() > 0) {
            openaiRequest.setResponseFormat(getJsonSchema(translateRequest.getChunkSize()));
        } else {
            messages.add(new OpenaiRequest.Message(MessageRole.system, "翻译每一项，返回输入的列表相同顺序和长度的结果。将翻译结果输出为string[]"));
        }

        // max message size
        if (translateRequest.getMessages().size() > this.messageLimit) {
            List<OpenaiRequest.Message> userMessages = translateRequest.getMessages().subList(translateRequest.getMessages().size() - this.messageLimit, translateRequest.getMessages().size());
            messages.addAll(userMessages);
        } else if (translateRequest.getMessages().isEmpty()) {
            throw new RuntimeException("messages cannot be null or empty");
        } else {
            messages.addAll(translateRequest.getMessages());
        }

        openaiRequest.setMessages(messages);

        String jsonBody = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(openaiRequest);
        log.info("jsonBody: {}", jsonBody);

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        // 构建请求
        Request request = new Request.Builder()
                .url(baseApi + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey) // 添加认证头
                .addHeader("Content-Type", "application/json") // 添加内容类型头
                .post(body) // 设置为 POST 请求
                .build();

        // 发送请求并处理响应
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                throw new IOException("Unexpected code " + response);
            }
        }
    }
    
    private Map<String, Object> getJsonSchema(int length) throws JsonProcessingException {
        String schema = """
                {
                    "type": "json_schema",
                    "json_schema": {
                      "schema": {
                        "items": {
                          "type": "string"
                        },
                        "minItems": %s
                      }
                    }
                  }""";
        return new ObjectMapper().readValue(schema.formatted(length), new TypeReference<>() {
        });
    }
}
