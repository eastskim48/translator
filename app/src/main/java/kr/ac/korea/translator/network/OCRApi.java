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

import javax.net.ssl.HttpsURLConnection;

import kr.ac.korea.translator.model.TextContainer;
import kr.ac.korea.translator.view.main.CoverActivity;

public class OCRApi{


    // Replace the subscriptionKey string value with your valid subscription key.
    static String subscriptionKey = "c2c8f85aeb454b368f851a6d1e36a6dd";

    static String host = "https://southeastasia.api.cognitive.microsoft.com", path = "/vision/v2.0/ocr?detectOrientation=true";
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
            String text, box = null;
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
                        text += wj.getAsJsonObject().get("text").getAsString() + " ";
                    }
                    Integer[] pos = parseBoundingBox(box);
                    TextContainer t = new TextContainer(pos, text);
                    resultArray.add(t);
                }
            }
            return resultArray;
        }

        private static Integer[] parseBoundingBox(String string){
            Integer[] rst = new Integer[4];
            String[] stringList = string.split(",");
            for(int i=0; i<4; i++){
                rst[i] = Integer.valueOf(stringList[i]);
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
        return stream.toByteArray();
    }

    public static void setLanguageParam(){
        params="&to=" + CoverActivity.getSelecteedLang();
    }

}