package com.github.charleslzq.faceengine.view

import io.fotoapparat.characteristic.LensPosition
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.*

sealed class InternalCamera(
        val lensPosition: LensPositionSelector,
        val configuration: CameraConfiguration
) {
    abstract fun withNewConfig(cameraPreviewConfiguration: CameraPreviewConfiguration): InternalCamera

    class Back(cameraPreviewConfiguration: CameraPreviewConfiguration) : InternalCamera(
            lensPosition = back(),
            configuration = CameraConfiguration(
                    previewResolution = cameraPreviewConfiguration.previewResolution,
                    previewFpsRange = highestFps(),
                    flashMode = off(),
                    focusMode = firstAvailable(
                            continuousFocusPicture(),
                            autoFocus()
                    )
            )
    ) {
        override fun withNewConfig(cameraPreviewConfiguration: CameraPreviewConfiguration) = Back(cameraPreviewConfiguration)
    }

    class Front(cameraPreviewConfiguration: CameraPreviewConfiguration) : InternalCamera(
            lensPosition = front(),
            configuration = CameraConfiguration(
                    previewResolution = cameraPreviewConfiguration.previewResolution,
                    previewFpsRange = highestFps(),
                    flashMode = off(),
                    focusMode = firstAvailable(
                            fixed(),
                            autoFocus()
                    )
            )
    ) {
        override fun withNewConfig(cameraPreviewConfiguration: CameraPreviewConfiguration) = Front(cameraPreviewConfiguration)
    }

    companion object {
        fun fromLensSelector(lensPosition: LensPosition, cameraPreviewConfiguration: CameraPreviewConfiguration) = when (lensPosition) {
            is LensPosition.Back -> Back(cameraPreviewConfiguration)
            is LensPosition.Front -> Front(cameraPreviewConfiguration)
            is LensPosition.External -> throw IllegalArgumentException("Unsupported Camera")
        }
    }
}