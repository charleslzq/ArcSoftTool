package com.github.charleslzq.faceengine.view.config

import io.fotoapparat.characteristic.LensPosition

sealed class CameraPreference {
    abstract val cameraId: String
    abstract val profile: String
    abstract val requestParameters: CameraParameters
}

class FotoCameraPreference(
        val lensPosition: LensPosition,
        override val cameraId: String,
        override val profile: String,
        override val requestParameters: FotoCameraParameters
) : CameraPreference()