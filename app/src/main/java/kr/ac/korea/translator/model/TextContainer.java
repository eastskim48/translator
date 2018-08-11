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
    /*
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
*/

}
