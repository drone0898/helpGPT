package kr.drone.helpgpt.data.model

import java.io.Serializable

data class Lecture(
    val id: String,                 // 아이디
    val number: String,             // 강좌번호
    val name: String,               // 강좌명
    val classfyName: String,        // 강좌분류
    val middleClassfyName: String,  // 강좌분류2
    val shortDescription: String,   // 짧은 설명
    val Org: String,            // 운영기관
    val teachers: String?,          // 교수진
    val overview: String?           // 상제정보(html)
) : Serializable {
    companion object {
        val EMPTY = Lecture(
            "", "", "", "","",  "", "", null, null
        )
    }
}