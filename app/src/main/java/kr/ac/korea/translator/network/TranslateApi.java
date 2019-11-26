package kr.ac.korea.translator.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class TranslateApi {

    static Gson gson;

    public static class RequestBody {
        String Text;
        public RequestBody(String text) {
            this.Text = text;
        }
    }

    public TranslateApi(){}

    public static String Post (URL url, String content, String key) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setRequestProperty("Content-Type", "application/json");;
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", key);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-ClientTraceId", java.util.UUID.randomUUID().toString());
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        byte[] encoded_content = content.getBytes("UTF-8");
        wr.write(encoded_content, 0, encoded_content.length);
        wr.flush();
        wr.close();
        StringBuilder response = new StringBuilder ();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return response.toString();
    }

    public static String Translate (String text, URL url, String key, JsonParser parser) throws Exception {
        gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        List<RequestBody> objList = new ArrayList<RequestBody>();
        objList.add(new RequestBody(text));
        return prettify(Post(url, gson.toJson(objList), key), parser);
    }

    public static String prettify(String json_text, JsonParser parser) {
        return gson.toJson(parser.parse(json_text));
    }

}