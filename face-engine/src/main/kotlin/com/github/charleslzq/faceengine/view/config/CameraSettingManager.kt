package com.github.charleslzq.faceengine.view.config

import com.github.charleslzq.faceengine.view.CameraPreviewOperator
import com.github.charleslzq.faceengine.view.FotoCameraOperatorSource
import com.github.charleslzq.faceengine.view.UVCCameraOperatorSource
import io.fotoapparat.parameter.Resolution
import java.util.concurrent.atomic.AtomicBoolean

class CameraSettingManager {
    private val updated = AtomicBoolean(false)
    private var cachedSetting = doLoadSetting()

    fun loadSetting() = if (updated.get()) {
        doLoadSetting().also {
            cachedSetting = it
        }
    } else {
        cachedSetting
    }

    fun loadParameters(camera: CameraPreviewOperator): CameraParameters = when (camera) {
        is FotoCameraOperatorSource.FotoCameraPreviewOperator -> FotoCameraParameters(Resolution(640, 480))
        is UVCCameraOperatorSource.UVCCameraOperator -> UVCCameraParameters(Resolution(640, 480))
        else -> throw IllegalArgumentException("Unsupported camera operator")
    }

    private fun doLoadSetting() = CameraSetting(
            CameraPreviewConfiguration(),
            emptyList()
    )
}