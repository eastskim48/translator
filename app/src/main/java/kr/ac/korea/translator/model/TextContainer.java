package kr.ac.korea.translator.model;

import com.google.api.services.vision.v1.model.BoundingPoly;
import com.google.api.services.vision.v1.model.Vertex;

import lombok.Data;

@Data
public class TextContainer {
    int x;
    int y;
    String rst;

    public TextContainer(BoundingPoly b, String text){
        Vertex v = b.getVertices().get(0);
        this.x = v.getX();
        this.y = v.getY();
        this.rst=text;
    }
}
