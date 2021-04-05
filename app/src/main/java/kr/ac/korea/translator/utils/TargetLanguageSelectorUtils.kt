package kr.ac.korea.translator.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kr.ac.korea.translator.R

object TargetLanguageSelectorUtils {
    private val FILE_NAME: String? = "LANG"
    private var codeMap:Map<String, String>? = null

    fun getSavedTargetLanguageIndex(context: Context): Int {
        val stringVal = getLanguageFromSharedPreference(context)
        val index = getSupportedLanguages(context).indexOf(stringVal)
        return if (index == -1) 0 else index
    }

    private fun getLanguageFromSharedPreference(context: Context): String? {
        return getSharedPreferences(context)?.getString("lang", "영어")
    }

    private fun getSharedPreferences(context: Context): SharedPreferences? {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    fun setLanguagePreferencesByIndex(context: Context, index: Int) {
        val supportedLanguages = getSupportedLanguages(context)
        if (index >= supportedLanguages.size) {
            Log.e("size error", "supportedLength outOfIndex Error")
            return
        }
        getSharedPreferences(context)?.edit()?.putString("lang", supportedLanguages[index])?.apply()
    }

    fun getSupportedLanguages(context: Context): Array<String> {
        return context.resources.getStringArray(R.array.arr_languages)
    }

    private fun getSupportedLanguageCodes(context: Context): Array<String> {
        return context.resources.getStringArray(R.array.arr_language_ocr)
    }

    fun getSavedTargetLanguageCode(context: Context): String? {
        val stringVal = getLanguageFromSharedPreference(context)
        val index = getSupportedLanguages(context).indexOf(stringVal)
        return if (index!=-1)  getSupportedLanguageCodes(context)[index] else "eng"
    }

    private fun createCodeMap(context:Context){
        val keys = context.resources.getStringArray(R.array.arr_language_ocr)
        val values = context.resources.getStringArray(R.array.arr_language_translator)
        codeMap = keys.zip(values).toMap()
    }
}