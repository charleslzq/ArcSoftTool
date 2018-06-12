package com.github.charleslzq.faceengine.view.config

data class CameraPreference(
        val cameraId: String,
        val source: String,
        val request: CameraPreviewRequest
)