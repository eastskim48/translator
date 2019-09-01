package kr.ac.korea.translator.model;

import lombok.Data;

@Data
public class TextContainer {
    int x;
    int y;
    String rst;

    public TextContainer(int x, int y, String text){
        this.x = x;
        this.y = y;
        this.rst=text;
    }
}
