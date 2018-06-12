package com.github.charleslzq.faceengine.view.config

import com.github.charleslzq.faceengine.view.CameraPreviewOperator
import com.github.charleslzq.faceengine.view.FotoCameraOperatorSource
import com.github.charleslzq.faceengine.view.UVCCameraOperatorSource
import io.fotoapparat.capability.Capabilities
import io.fotoapparat.parameter.Resolution

fun CameraPreviewOperator.getCapabilities(capabilities: Capabilities) = CameraCapabilities.from(this, capabilities)

sealed class CameraCapabilities {
    abstract val previewResolutions: List<Resolution>

    companion object {
        fun from(camera: CameraPreviewOperator, capabilities: Capabilities) = when (camera) {
            is FotoCameraOperatorSource.FotoCameraPreviewOperator -> FotoCameraCapabilities.transform(capabilities)
            is UVCCameraOperatorSource.UVCCameraOperator -> UVCCameraCapabilities.transform(capabilities)
            else -> throw IllegalArgumentException("Unsupported camera operator type")
        }
    }
}

data class FotoCameraCapabilities(
        override val previewResolutions: List<Resolution>
) : CameraCapabilities() {
    companion object : Transformer<Capabilities, FotoCameraCapabilities> {
        override fun transform(data: Capabilities) = FotoCameraCapabilities(data.previewResolutions.toList())
    }
}

data class UVCCameraCapabilities(
        override val previewResolutions: List<Resolution>
) : CameraCapabilities() {
    companion object : Transformer<Capabilities, UVCCameraCapabilities> {
        override fun transform(data: Capabilities) = UVCCameraCapabilities(data.previewResolutions.toList())
    }
}