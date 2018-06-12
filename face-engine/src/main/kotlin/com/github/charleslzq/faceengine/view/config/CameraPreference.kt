package com.github.charleslzq.faceengine.view.config

sealed class CameraPreference {
    abstract val cameraId: String
    abstract val source: String
    abstract val profile: String
    abstract val request: CameraPreviewRequest
}

class FotoCameraPreference(
        override val cameraId: String,
        override val source: String,
        override val profile: String,
        override val request: FotoCameraPreviewRequest
) : CameraPreference()