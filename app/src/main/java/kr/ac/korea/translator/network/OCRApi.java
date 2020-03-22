package kr.ac.korea.translator.network;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import kr.ac.korea.translator.model.TextContainer;
import kr.ac.korea.translator.view.main.CoverActivity;

public class OCRApi{

    static String host = "https://api.ocr.space/parse/image";

    private static class OCRTask extends AsyncTask<Void, String, String>{
        private String data;
        private CoverActivity.TranslateCallback callback;
        private String apiKey;
        private static JsonObject wordobj;

        public OCRTask(String data, CoverActivity.TranslateCallback callback, String key){
            this.data = data;
            this.callback = callback;
            this.apiKey = key;
        }
        @Override
        protected String doInBackground(Void... v) {
            List<TextContainer> result = null;
            try {
                String encodedData = this.data;
                URL url = new URL(host);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000); //늘려야 할 수도
                connection.setRequestMethod("POST");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                connection.setReadTimeout(10000);

                JSONObject postDataParams = new JSONObject();

                postDataParams.put("apikey", this.apiKey); //TODO Add your Registered API key
                postDataParams.put("base64Image", "data:image/jpeg;base64," + encodedData);
                postDataParams.put("language", "eng");
                postDataParams.put("isOverlayRequired", "true");

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(getPostDataString(postDataParams));
                wr.flush();
                wr.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null)
                {
                    response.append(inputLine);
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
            JsonParser parser = new JsonParser();
            JsonObject parsedResult = parser.parse(json_text).getAsJsonObject().get("ParsedResults").getAsJsonArray().get(0).getAsJsonObject();
            JsonArray lines = parsedResult.get("TextOverlay").getAsJsonObject().get("Lines").getAsJsonArray();
            List<TextContainer> resultArray = new ArrayList<>();

            // line 단위로 번역
            for(JsonElement line:lines){
                // Words 단위 Parsing
                /*
                JsonArray words = j.getAsJsonObject().get("Words").getAsJsonArray();
                for(JsonElement lj:words){
                    wordobj = lj.getAsJsonObject();
                    text = wordobj.get("WordText").toString();
                    Integer[] pos = {getCordVal("Left"), getCordVal("Top"), getCordVal("Width"), getCordVal("Height")};
                    TextContainer t = new TextContainer(pos, text);
                    resultArray.add(t);
                }
                */
                // Line 단위 Parsing
                JsonObject container = line.getAsJsonObject();
                text = container.get("LineText").toString();
                wordobj = container.get("Words").getAsJsonArray().get(0).getAsJsonObject();
                Integer[] pos = {getCordVal("Left"), getCordVal("Top"), getCordVal("Width"), getCordVal("Height")};
                TextContainer t = new TextContainer(pos, text);
                resultArray.add(t);
            }
            return resultArray;
        }

        private static Integer getCordVal(String key){
            return (int)Float.parseFloat(wordobj.get(key).toString());
        }

        public String getPostDataString(JSONObject params) throws Exception {

            StringBuilder result = new StringBuilder();
            boolean first = true;
            Iterator<String> itr = params.keys();

            while (itr.hasNext()) {
                String key = itr.next();
                Object value = params.get(key);
                if (first)
                    first = false;
                else
                    result.append("&");
                result.append(URLEncoder.encode(key, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(value.toString(), "UTF-8"));

            }
            return result.toString();
        }
    }

    public static void Post (Bitmap imgData, CoverActivity.TranslateCallback callback, String key) throws Exception {
        byte[] data = bitmapToByteArray(imgData);
        String encoded = Base64.encodeToString(data, Base64.NO_WRAP);
        OCRTask ocrTask = new OCRTask(encoded, callback, key);
        ocrTask.execute().get();
    }

    public static byte[] bitmapToByteArray( Bitmap $bitmap ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        $bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}