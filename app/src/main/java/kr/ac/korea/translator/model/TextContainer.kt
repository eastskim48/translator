package kr.ac.korea.translator.model

data class TextContainer(var pos: Array<Int?>, var text: String?) {
    var x: Int = pos[0]!!
    var y: Int = pos[1]!!
    var w: Int = pos[2]!!
    var h: Int = pos[3]!!
    var rst: String? = text
}