package kr.ac.korea.translator.model;

import java.util.List;

import lombok.Data;

@Data
public class Detection {
    @Data
    public class detectedLanguage{
        public String language;
        public double score;
    }
    @Data
    public class t{
        public List<translation> translations;
    }
    @Data
    public class translation{
        public String text;
        public String to;
    }
}
