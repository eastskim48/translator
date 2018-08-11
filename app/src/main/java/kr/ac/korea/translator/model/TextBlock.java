package kr.ac.korea.translator.model;

import com.google.api.services.vision.v1.model.BoundingPoly;
import com.google.api.services.vision.v1.model.Vertex;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TextBlock {
    List<Integer> x;
    List<Integer> y;
    String rst;
    public TextBlock(BoundingPoly boundingPoly,String rst){
        x = new ArrayList<>();
        y = new ArrayList<>();
        this.rst=rst;
        for(Vertex v:boundingPoly.getVertices()) {
            this.x.add(v.getX());
            this.y.add(v.getY());
        }
    }
}
