package kr.ac.korea.translator.model

data class TranslateResponse(var detectedLanguage: TranslateResponse.DetectedLanguage?, var translations: MutableList<Translation?>?){

    data class DetectedLanguage(var language:String?, var score:Float=0f){
    }

    data class Translation(var text:String?, var to:String?) {
    }
}