package com.github.charleslzq.face.baidu

data class DetectedFace(
        val faceToken: String,
        val location: FaceLocation,
        val faceProbability: Double,
        val angle: FaceAngle,
        val age: Double?,
        val beauty: Double?,
        val expression: DetectedItem<Expression>?,
        val shape: DetectedItem<Shape>?,
        val gender: DetectedItem<Gender>?,
        val glasses: DetectedItem<Glasses>?,
        val race: DetectedItem<Race>?,
        val faceType: DetectedItem<FaceType>?,
        val landmark: List<FacePoint>?,
        val landmark72: List<FacePoint>?,
        val quality: Quality?,
        val parsingInfo: String?
)

data class FacePoint(
        val x: Double,
        val y: Double
)

data class FaceLocation(
        val left: Double,
        val top: Double,
        val width: Double,
        val height: Double,
        val rotation: Int
)

data class FaceAngle(
        val yaw: Double,
        val pitch: Double,
        val roll: Double
)

data class DetectedItem<T : Enum<T>>(
        val type: T,
        val probability: Double
)

enum class Expression {
    NONE,
    SMILE,
    LAUGH
}

enum class Shape {
    SQUARE,
    TRIANGLE,
    OVAL,
    HEART,
    ROUND
}

enum class Gender {
    MALE,
    FEMALE
}

enum class Glasses {
    NONE,
    COMMON,
    SUN
}

enum class Race {
    YELLOW,
    WHITE,
    BLACK,
    ARABS
}

enum class FaceType {
    HUMAN,
    CARTOON
}

data class Quality(
        val occlusion: Occlusion?,
        val blur: Double,
        val illumination: Double,
        val completeness: Int
)

data class Occlusion(
        val leftEye: Double,
        val rightEye: Double,
        val nose: Double,
        val mouth: Double,
        val leftCheek: Double,
        val rightCheek: Double,
        val chin: Double
)

data class QueriedUser(
        val groupId: String,
        val userInfo: String
)

data class FaceListItem(
        val faceToken: String,
        val createTime: String
)

data class FaceToken(
        val faceToken: String
)

data class VerifyThreshold(
        val err10000th: Float,
        val err1000th: Float,
        val err100th: Float
)