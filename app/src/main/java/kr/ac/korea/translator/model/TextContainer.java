package kr.ac.korea.translator.model;

import android.util.Log;

import com.google.api.services.vision.v1.model.BoundingPoly;
import com.google.api.services.vision.v1.model.Vertex;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import kr.ac.korea.translator.network.TranslateApi;
import lombok.Data;

@Data
public class TextContainer {
    List<Integer> inX;
    List<Integer> inY;
    List<String> textArray;
    List<String> rst;
    List<Integer> outX;
    List<Integer> outY;

    public TextContainer(){
        inX = new ArrayList<>();
        inY = new ArrayList<>();
        textArray = new ArrayList<>();
        rst = new ArrayList<>();
        outX = new ArrayList<>();
        outY = new ArrayList<>();
    }

    public void addItem(BoundingPoly boundingPoly, String blockText){
        Vertex v = boundingPoly.getVertices().get(0);
        inX.add(v.getX());
        inY.add(v.getY());
        textArray.add(blockText);
    }

    public void translate(){
        List<Detection> result=null;
        try{
            String s = TranslateApi.Translate(textArray);
            Log.e("s",s);
            Gson gson = new Gson();
            result = gson.fromJson(s, new TypeToken<List<Detection>>() {}.getType());
        }
        catch(Exception e){
            Log.e("Exception",e.toString());
        }
        if(result!=null){
            int i=0;
            for (Detection d : result) {
                if (!d.getDetectedLanguage().getLanguage().equals("ko")) {
                    rst.add(d.getTranslations().get(0).getText());
                    outX.add(inX.get(i));
                    outY.add(inY.get(i));
                }
                i++;
            }
        }
    }


}
