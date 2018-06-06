package com.github.charleslzq.faceengine.view

import io.fotoapparat.characteristic.LensPosition
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.*

sealed class InternalCamera(
        val lensPosition: LensPositionSelector,
        val configuration: CameraConfiguration
) {
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
    )

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
    )

    companion object {
        fun fromLensSelector(lensPosition: LensPosition, cameraPreviewConfiguration: CameraPreviewConfiguration) = when (lensPosition) {
            is LensPosition.Back -> Back(cameraPreviewConfiguration)
            is LensPosition.Front -> Front(cameraPreviewConfiguration)
            is LensPosition.External -> throw IllegalArgumentException("Unsupported Camera")
        }
    }
}