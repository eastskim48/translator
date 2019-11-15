package kr.ac.korea.translator.network;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import kr.ac.korea.translator.model.TextContainer;
import kr.ac.korea.translator.view.main.CoverActivity;

/*
 * Gson: https://github.com/google/gson
 * Maven info:
 *     groupId: com.google.code.gson
 *     artifactId: gson
 *     version: 2.8.1
 */

/* NOTE: To compile and run this code:
1. Save this file as Translate.java.
2. Run:
    javac Translate.java -cp .;gson-2.8.1.jar -encoding UTF-8
3. Run:
    java -cp .;gson-2.8.1.jar Translate
*/

public class OCRApi{

// **********************************************
// *** Update or verify the following values. ***
// **********************************************

    // Replace the subscriptionKey string value with your valid subscription key.
    static String subscriptionKey = "c2c8f85aeb454b368f851a6d1e36a6dd";

    static String host = "https://southeastasia.api.cognitive.microsoft.com";
    static String path = "/vision/v2.0/ocr?detectOrientation=true";
    static String params;



    public static class RequestBody {
        byte[] data;

        public RequestBody(byte[] data) {
            this.data = data;
        }
    }

    private static class OCRTask extends AsyncTask<Void, String, String>{
        private byte[] data;
        private CoverActivity.TranslateCallback callback;

        public OCRTask(byte[] data, CoverActivity.TranslateCallback callback){
            this.data = data;
            this.callback = callback;
        }
        @Override
        protected String doInBackground(Void... v) {
            List<TextContainer> result = null;
            try {
                byte[] dataBytes = this.data;
                URL url = new URL(host + path + params);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000); //늘려야 할 수도
                connection.setRequestProperty("Content-Type", "application/octet-stream");
                connection.setRequestProperty("Content-Length", dataBytes.length + "");
                connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);
                connection.setRequestMethod("POST");
                connection.setReadTimeout(10000);
                OutputStream outputStream = connection.getOutputStream();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(dataBytes);
                byte[] buffer = new byte[4096];
                int bytesRead = -1;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();
                StringBuilder response = new StringBuilder();

                Map<String, List<String>> map = connection.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                    Log.e("se", "Key : " + entry.getKey()
                            + " ,Value : " + entry.getValue());
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                result = parse(response.toString());
                this.callback.resultToScreen(result);

            }
            catch (Exception e){
                Log.e("error", e.toString());
            }
            return null;
        }

        private static List<TextContainer> parse(String json_text) {
            String text = null;
            String box = null;
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(json_text);
            JsonObject objects = json.getAsJsonObject();
            JsonArray arr = objects.get("regions").getAsJsonArray();
            List<TextContainer> resultArray = new ArrayList<>();

            // line 단위로 번역
            for(JsonElement j:arr){
                JsonArray linesArr = j.getAsJsonObject().get("lines").getAsJsonArray();
                for(JsonElement lj:linesArr){
                    JsonObject obj = lj.getAsJsonObject();
                    box = obj.get("boundingBox").getAsString();
                    text = "";
                    JsonArray wordsArr = obj.get("words").getAsJsonArray();
                    for(JsonElement wj : wordsArr) {
                        text += wj.getAsJsonObject().get("text").getAsString();
                        text += " ";
                    }
                    // Patterns.WEB_URL.matcher(text).matches()
                    Integer[] pos = parseBoundingBox(box);
                    TextContainer t = new TextContainer(pos, text);
                    resultArray.add(t);
                }
            }

            //word 단위로 번역

            /*
            for(JsonElement j:arr){
                JsonArray linesArr = j.getAsJsonObject().get("lines").getAsJsonArray();
                for(JsonElement lj:linesArr){
                    JsonArray wordsArr = lj.getAsJsonObject().get("words").getAsJsonArray();
                    for(JsonElement wj : wordsArr){
                        //url skip 추가해야 함
                        JsonObject obj = wj.getAsJsonObject();
                        text = obj.get("text").getAsString();
                        box = obj.get("boundingBox").getAsString();
                        Integer[] pos = parseBoundingBox(box);
                        TextContainer t = new TextContainer(pos[0],pos[1], text);
                        resultArray.add(t);
                    }
                }

            }
            */
            return resultArray;
        }

        private static Integer[] parseBoundingBox(String string){
            Integer[] rst = new Integer[4];
            try{
                String[] stringList = string.split(",");
                for(int i=0; i<4; i++){
                    rst[i] = Integer.valueOf(stringList[i]);
                }
            }
            catch(Exception e){
                Log.e("e", e.toString());
            }
            return rst;
        }
    }


    public static void Post (Bitmap imgData, CoverActivity.TranslateCallback callback) throws Exception {

        setLanguageParam();

        List<RequestBody> objList = new ArrayList<>();
        objList.add(new OCRApi.RequestBody(bitmapToByteArray(imgData)));
        byte[] data = bitmapToByteArray(imgData);
        OCRTask ocrTask = new OCRTask(data, callback);
        ocrTask.execute().get();
    }

    public static byte[] bitmapToByteArray( Bitmap $bitmap ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        $bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray() ;
        return byteArray ;
    }
    public static void setLanguageParam(){
        params="&to=" + CoverActivity.getSelecteedLang();
    }

}