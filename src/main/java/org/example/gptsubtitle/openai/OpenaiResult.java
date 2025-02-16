package org.example.gptsubtitle.openai;

import lombok.Data;

import java.util.List;

@Data
public class OpenaiResult {
    private String id;
    private List<Choice> choices;

    @Data
    public static class Choice {
        private int index;
        private OpenaiRequest.Message message;
    }
}
