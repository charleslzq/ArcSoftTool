package com.github.charleslzq.faceengine.view.config

import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.Resolution

sealed class CameraParameters {
    abstract val resolution: Resolution
}

class FotoCameraParameters(
        override val resolution: Resolution
) : CameraParameters() {
    fun toFotoCameraConfiguration() = CameraConfiguration.builder()
            .previewResolution {
                firstOrNull { it == resolution }
            }
            .build()
}

class UVCCameraParameters(
        override val resolution: Resolution
) : CameraParameters()