package kr.ac.korea.translator.model;

import lombok.Data;

@Data
public class TextContainer {
    int x;
    int y;
    int w;
    int h;
    String rst;

    public TextContainer(Integer[] pos, String text){
        this.x = pos[0];
        this.y = pos[1];
        this.w = pos[2];
        this.h = pos[3];
        this.rst=text;
    }
}
