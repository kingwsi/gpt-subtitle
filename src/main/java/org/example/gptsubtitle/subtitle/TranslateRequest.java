package org.example.gptsubtitle.subtitle;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.gptsubtitle.config.AppConfig;
import org.example.gptsubtitle.openai.OpenaiRequest;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TranslateRequest extends OpenaiRequest {
    private List<Message> systemMessages = new ArrayList<>();
    
    private AppConfig config;
    
    private int chunkSize;
    
    private boolean jsonSchema;
    
    public void addSystemMessage(Message message) {
        this.systemMessages.add(message);
    }
    
}
