package com.github.charleslzq.face.baidu.data

data class DetectOptions(
        val maxCount: Int = 1,
        val source: FaceSource = FaceSource.LIVE,
        val fields: List<FaceField> = emptyList(),
        val complete: Boolean = true
) {
    fun withNewMaxCount(maxCount: Int) = copy(
            maxCount = maxCount
    )

    fun withNewSource(faceSource: FaceSource) = copy(
            source = faceSource
    )

    fun withNewFiels(vararg fields: FaceField) = copy(
            fields = fields.toList()
    )

    fun withNewFiels(fields: List<FaceField>) = copy(
            fields = fields
    )

    fun withNewComplete(complete: Boolean) = copy(
            complete = complete
    )
}

data class SearchOptions(
        val groups: List<String> = emptyList(),
        val maxUser: Int = 1,
        val quality: QualityControl = QualityControl.NONE,
        val liveness: LivenessControl = LivenessControl.NONE
) {
    fun withNewGroups(vararg groups: String) = copy(
            groups = groups.toList()
    )

    fun withNewGroups(groups: List<String>) = copy(
            groups = groups
    )

    fun withNewQuality(quality: QualityControl) = copy(
            quality = quality
    )

    fun withNewMaxUser(maxUser: Int) = copy(
            maxUser = maxUser
    )

    fun withNewLiveness(liveness: LivenessControl) = copy(
            liveness = liveness
    )
}