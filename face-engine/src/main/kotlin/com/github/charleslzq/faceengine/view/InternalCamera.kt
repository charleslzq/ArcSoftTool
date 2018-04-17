package com.github.charleslzq.faceengine.view

import io.fotoapparat.characteristic.LensPosition
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.*

sealed class InternalCamera(
        val lensPosition: LensPositionSelector,
        val configuration: CameraConfiguration
) {

    object Back : InternalCamera(
            lensPosition = back(),
            configuration = CameraConfiguration(
                    previewResolution = firstAvailable(
                            wideRatio(highestResolution()),
                            standardRatio(highestResolution())
                    ),
                    previewFpsRange = highestFps(),
                    flashMode = off(),
                    focusMode = firstAvailable(
                            continuousFocusPicture(),
                            autoFocus()
                    )
            )
    )

    object Front : InternalCamera(
            lensPosition = front(),
            configuration = CameraConfiguration(
                    previewResolution = firstAvailable(
                            wideRatio(highestResolution()),
                            standardRatio(highestResolution())
                    ),
                    previewFpsRange = highestFps(),
                    flashMode = off(),
                    focusMode = firstAvailable(
                            fixed(),
                            autoFocus()
                    )
            )
    )

    companion object {
        fun fromLensSelector(lensPosition: LensPosition) = when (lensPosition) {
            is LensPosition.Back -> Back
            is LensPosition.Front -> Front
            is LensPosition.External -> throw IllegalArgumentException("Unsupported Camera")
        }
    }
}