package org.example.gptsubtitle.subtitle;

import lombok.Data;

/**
 * description:  <br>
 * date: 2025/2/16 18:15 <br>
 */
@Data
public class TranslatedMapParam {
    private String fileId;
    private Integer subtitleIndex;
    private String translatedText;
}
