package kr.ac.korea.translator.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import kr.ac.korea.translator.R;

public class TargetLanguageSelectorUtils {
    private static final String FILE_NAME = "LANG";


    public TargetLanguageSelectorUtils(){
    }

    public static int getSavedTargetLanguageIndex(Context context){
        String stringVal = getLanguageFromSharedPreference(context);
        int index = getSupportedLanguages(context).indexOf(stringVal);
        return index == -1 ? 0 : index;
    }

    public static String getLanguageFromSharedPreference(Context context){
        return getSharedPreferences(context).getString("lang","영어");
    }

    public static SharedPreferences getSharedPreferences(Context context){
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public static void setLanguagePreferencesByIndex(Context context, int index){
        List<String> supportedLanguages = getSupportedLanguages(context);
        if(index >= supportedLanguages.size()){
            Log.e("size error","supportedLength outOfIndex Error");
            return;
        }
        getSharedPreferences(context).edit().putString("lang", supportedLanguages.get(index)).apply();
    }

    public static List<String> getSupportedLanguages(Context context){
        return Arrays.asList(context.getResources().getStringArray(R.array.arr_languages));
    }

    public static List<String> getSupportedLanguageCodes(Context context){
        return Arrays.asList(context.getResources().getStringArray(R.array.arr_language_codes));
    }

    public static String getSavedTargetLanguageCode(Context context){
        String stringVal = getLanguageFromSharedPreference(context);
        int index = getSupportedLanguages(context).indexOf(stringVal);
        String code = null;
        if(index!=-1) {
            code = getSupportedLanguageCodes(context).get(index);
        }
        return code == null? "eng" : code;
    }
}
