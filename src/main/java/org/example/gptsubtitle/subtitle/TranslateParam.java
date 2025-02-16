package org.example.gptsubtitle.subtitle;

import lombok.Data;
import org.example.gptsubtitle.config.AppConfig;

@Data
public class TranslateParam {
    private String fileId;
    private Integer begin;
    private AppConfig config;
}
