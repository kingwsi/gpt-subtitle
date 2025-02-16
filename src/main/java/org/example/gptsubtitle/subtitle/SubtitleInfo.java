package org.example.gptsubtitle.subtitle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubtitleInfo {
    private String fileId;
    private List<Subtitle> subtitles;
}
