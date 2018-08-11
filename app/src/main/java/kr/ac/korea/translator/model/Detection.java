package kr.ac.korea.translator.model;

import java.util.List;

import lombok.Data;

@Data
public class Detection {
    public DetectedLanguage detectedLanguage;
    public List<Translation> translations;
    @Data
    public class DetectedLanguage{
        public String language;
        public double score;
    }
    @Data
    public class Translation{
        public String text;
        public String to;
    }
}
