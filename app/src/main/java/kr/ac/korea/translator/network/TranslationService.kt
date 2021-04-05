package kr.ac.korea.translator.network

import kr.ac.korea.translator.model.TranslateRequest
import kr.ac.korea.translator.model.TranslateResponse
import retrofit2.Call
import retrofit2.http.*

interface TranslationService {
    @POST("/translate?api-version=3.0&to=ko")
    @Headers(
            "Content-Type:application/json",
            "charset:UTF-8"
    )
    fun translate(
            @Header("Ocp-Apim-Subscription-Key") key:String,
            @Header("X-ClientTraceId") uuid:String,
            @Body data:List<TranslateRequest>
    ): Call<List<TranslateResponse>>

}