package com.github.charleslzq.faceengine.view.config

import com.github.charleslzq.faceengine.view.CameraPreviewOperator
import com.github.charleslzq.faceengine.view.FotoCameraOperatorSource
import com.github.charleslzq.faceengine.view.UVCCameraOperatorSource
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.parameter.camera.CameraParameters as IoFotoCameraParameters

fun CameraPreviewOperator.getCurrentParameters(data: IoFotoCameraParameters) = CameraParameters.from(this, data)

sealed class CameraParameters {
    abstract val resolution: Resolution

    companion object {
        fun from(camera: CameraPreviewOperator, data: IoFotoCameraParameters) = when (camera) {
            is FotoCameraOperatorSource.FotoCameraPreviewOperator -> FotoCameraParameters.transform(data)
            is UVCCameraOperatorSource.UVCCameraOperator -> UVCCameraParameters.transform(data)
            else -> throw IllegalArgumentException("Unsupported camera operator type")
        }
    }
}

class FotoCameraParameters(
        override val resolution: Resolution
) : CameraParameters() {
    companion object : Transformer<IoFotoCameraParameters, FotoCameraParameters> {
        override fun transform(data: IoFotoCameraParameters) = FotoCameraParameters(data.previewResolution)
    }
}

class UVCCameraParameters(
        override val resolution: Resolution
) : CameraParameters() {
    companion object : Transformer<IoFotoCameraParameters, UVCCameraParameters> {
        override fun transform(data: IoFotoCameraParameters) = UVCCameraParameters(data.previewResolution)
    }
}