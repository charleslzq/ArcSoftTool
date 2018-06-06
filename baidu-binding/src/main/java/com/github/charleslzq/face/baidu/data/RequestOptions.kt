package com.github.charleslzq.face.baidu.data

data class DetectOptions(
        val maxCount: Int = 1,
        val source: FaceSource = FaceSource.LIVE,
        val fields: List<FaceField> = emptyList(),
        val complete: Boolean = true
)

data class SearchOptions(
        val groups: List<String> = emptyList(),
        val maxUser: Int = 1,
        val quality: QualityControl = QualityControl.NONE,
        val liveness: LivenessControl = LivenessControl.NONE
)