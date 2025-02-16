package org.example.gptsubtitle.subtitle;

import lombok.Data;

/**
 * description:  <br>
 * date: 2025/2/9 15:13 <br>
 */
@Data
public class Subtitle {
    private int realIndex;
    private int index;
    private String timestamp;
    private String text;
    private String translatedText;
    private boolean translated;

    public Subtitle(int index, String timestamp, String text) {
        this.index = index;
        this.timestamp = timestamp;
        this.text = text;
    }
}