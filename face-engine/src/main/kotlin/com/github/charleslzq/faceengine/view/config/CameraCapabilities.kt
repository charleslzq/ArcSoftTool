package com.github.charleslzq.faceengine.view.config

import io.fotoapparat.parameter.Resolution

sealed class CameraCapabilities {
    abstract val previewResolutions: List<Resolution>
}

class FotoCameraCapabilities(
        override val previewResolutions: List<Resolution>
) : CameraCapabilities()

class UVCCameraCapabilities(
        override val previewResolutions: List<Resolution>
) : CameraCapabilities()