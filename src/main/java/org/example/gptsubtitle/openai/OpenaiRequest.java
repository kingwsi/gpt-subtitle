package org.example.gptsubtitle.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class OpenaiRequest {
    private String requestId;
    private String model;
    private List<Message> messages = new ArrayList<>();
    
    @JsonProperty("max_tokens")
    private Integer maxTokens = 4096;
    
    private boolean stream;
    
    @JsonProperty("response_format")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> responseFormat;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private MessageRole role;
        private String content;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }
}


