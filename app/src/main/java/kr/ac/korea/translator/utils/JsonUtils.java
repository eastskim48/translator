package kr.ac.korea.translator.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class JsonUtils {

    public static String objectToJson(Object object){
        Gson gson = new Gson();
        return gson.toJson(object);
    }
    public static <T> T JsonToObject(String json, Class<T> clazz) {
        Gson gson = new Gson();
        T object = gson.fromJson(json,clazz);
        return object;
    }
    public static <T> List<T> JsonToList(String json, Class<T> clazz) {
        Gson gson = new Gson();
        List<T> object = gson.fromJson(json,new TypeToken<List<T>>(){}.getType());
        return object;
    }
}
