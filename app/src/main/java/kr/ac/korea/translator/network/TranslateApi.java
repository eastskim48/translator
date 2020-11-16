package kr.ac.korea.translator.network;

import android.content.Context;
import android.util.Log;

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

import kr.ac.korea.translator.R;

public class TranslateApi {

    static Gson gson;
    static URL url;
    static String key;

    public static class RequestBody {
        String Text;
        public RequestBody(String text) {
            this.Text = text;
        }
    }

    public TranslateApi(Context context){
        try{
            url = new URL (context.getString(R.string.translate_api_path) + "&to=ko");
        }
        catch(Exception e){
            Log.e("translate URL exception", e.toString());
        }
        key = context.getString(R.string.translate_key);
    }

    public String Post (String content) throws Exception {
        HttpsURLConnection connection = getConnection();
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

    public HttpsURLConnection getConnection(){
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setRequestProperty("Content-Type", "application/json");
            ;
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", key);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("X-ClientTraceId", java.util.UUID.randomUUID().toString());
        }
        catch(Exception e){
            Log.e("tranlate setConn excep", e.toString());
        }
        return connection;
    }

    public String Translate (String text) throws Exception {
        gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        List<RequestBody> objList = new ArrayList<RequestBody>();
        objList.add(new RequestBody(text));
        return prettify(Post(gson.toJson(objList)));
    }

    public String prettify(String json_text) {
        JsonParser parser = new JsonParser();
        return gson.toJson(parser.parse(json_text));
    }

}