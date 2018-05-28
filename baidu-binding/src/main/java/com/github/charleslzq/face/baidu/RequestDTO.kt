package com.github.charleslzq.face.baidu

data class Image(
        val type: Type = Type.BASE64,
        val data: String = ""
) {
    enum class Type {
        BASE64,
        URL,
        FACE_TOKEN
    }
}

enum class FaceField {
    AGE,
    BEAUTY,
    EXPRESSION,
    FACE_SHAPE,
    GENDER,
    GLASSES,
    LANDMARK,
    RACE,
    QUALITY,
    FACE_TYPE,
    PARSING
}

enum class FaceSource {
    LIVE,
    IDCARD,
    WATERMARK,
    CERT
}

data class UserMeta(
        val groupId: String,
        val userId: String,
        val userInfo: String? = null
)

enum class QualityControl {
    NONE,
    LOW,
    NORMAL,
    HIGH
}

enum class LivenessControl {
    NONE,
    LOW,
    NORMAL,
    HIGH
}

data class MatchReq(
        val image: Image = Image(),
        val source: FaceSource = FaceSource.LIVE,
        val quality: QualityControl = QualityControl.NONE,
        val liveness: LivenessControl = LivenessControl.NONE
)

data class FaceVerifyReq(
        val image: Image = Image(),
        val complete: Boolean = false,
        val fields: List<FaceField> = emptyList()
)

