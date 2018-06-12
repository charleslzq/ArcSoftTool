package com.github.charleslzq.faceengine.view.config

import io.fotoapparat.parameter.Resolution

sealed class CameraParameters {
    abstract val resolution: Resolution
}

class FotoCameraParameters(
        override val resolution: Resolution
) : CameraParameters()

class UVCCameraParameters(
        override val resolution: Resolution
) : CameraParameters()