package kr.ac.korea.translator.network

import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kr.ac.korea.translator.R
import kr.ac.korea.translator.model.TextContainer
import kr.ac.korea.translator.utils.TargetLanguageSelectorUtils
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.jvm.Throws

class OCRApi(context: Context) {
    private class OCRTask(private val data: String?, private val callback: ((MutableList<TextContainer>)->(Unit)), private val apiKey: String?, private val languageToRecognize: String?) : AsyncTask<Void?, String?, String?>() {
        override fun doInBackground(vararg v: Void?): String? {
            try {
                val postDataParams = JSONObject()
                postDataParams.put("apikey", apiKey) //TODO Add your Registered API key
                postDataParams.put("base64Image", "data:image/jpeg;base64,$data")
                postDataParams.put("language", languageToRecognize)
                postDataParams.put("isOverlayRequired", "true")
                val connection = setConnection()
                val wr = DataOutputStream(connection?.outputStream)
                wr.writeBytes(getPostDataString(postDataParams))
                wr.flush()
                wr.close()
                val `in` = BufferedReader(InputStreamReader(connection?.inputStream, "UTF-8"))
                var inputLine: String?
                val response = StringBuffer()
                while (`in`.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                `in`.close()
                val result = parse(response.toString())
                result?.apply{
                    callback(this)
                }
            } catch (e: Exception) {
                Log.e("error", e.toString())
            }
            return null
        }

        private fun setConnection(): HttpsURLConnection? {
            var connection: HttpsURLConnection? = null
            try {
                connection = url?.openConnection() as HttpsURLConnection
                connection.apply{
                    this.doOutput = true
                    this.connectTimeout = 10000 //늘려야 할 수도
                    this.requestMethod = "POST"
                    this.setRequestProperty("User-Agent", "Mozilla/5.0")
                    this.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
                    this.readTimeout = 10000
                }
            } catch (e: Exception) {
                Log.e("OCR setConnection excep", e.toString())
            }
            return connection
        }

        @Throws(Exception::class)
        fun getPostDataString(params: JSONObject): String? {
            val result = StringBuilder()
            var first = true
            val itr = params.keys()
            while (itr.hasNext()) {
                val key = itr.next()
                val value = params.get(key)
                if (first) first = false else result.append("&")
                result.append(URLEncoder.encode(key, "UTF-8"))
                result.append("=")
                result.append(URLEncoder.encode(value.toString(), "UTF-8"))
            }
            return result.toString()
        }

        companion object {
            private var wordobj: JsonObject? = null
            private fun parse(json_text: String?): MutableList<TextContainer>? {
                val parsedResult = JsonParser.parseString(json_text).asJsonObject["ParsedResults"].asJsonArray[0].asJsonObject
                val lines = parsedResult["TextOverlay"].asJsonObject["Lines"].asJsonArray
                val resultArray: MutableList<TextContainer> = ArrayList()

                // line 단위로 번역
                for (line in lines) {
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
                    val container = line.asJsonObject
                    var text = container["LineText"].toString()
                    wordobj = container["Words"].asJsonArray[0].asJsonObject
                    val pos = arrayOf(getCordVal("Left"), getCordVal("Top"), getCordVal("Width"), getCordVal("Height"))
                    val t = TextContainer(pos, text)
                    resultArray.add(t)
                }
                return resultArray
            }

            private fun getCordVal(key: String?): Int? {
                return wordobj!![key].asInt
            }
        }

    }

    @Throws(Exception::class)
    fun post(imgData: Bitmap?, callback: ((MutableList<TextContainer>)->(Unit))) {
        val data = bitmapToByteArray(imgData)
        val encoded = Base64.encodeToString(data, Base64.NO_WRAP)
        val ocrTask = OCRTask(encoded, callback, key, languageToRecognize)
        ocrTask.execute().get()
    }

    companion object {
        var url: URL? = null
        var key: String? = null
        var languageToRecognize: String? = null
        fun bitmapToByteArray(`$bitmap`: Bitmap?): ByteArray? {
            val stream = ByteArrayOutputStream()
            `$bitmap`?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            return stream.toByteArray()
        }
    }

    init {
        try {
            url = URL(context.getString(R.string.ocr_api_path))
        } catch (e: Exception) {
            Log.e("OCR URL exception", e.toString())
        }
        key = context.getString(R.string.ocr_key)
        languageToRecognize = TargetLanguageSelectorUtils.getSavedTargetLanguageCode(context)
    }
}