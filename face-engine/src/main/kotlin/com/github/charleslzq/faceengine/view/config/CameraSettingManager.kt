package com.github.charleslzq.faceengine.view.config

import com.github.charleslzq.faceengine.view.CameraPreviewOperator
import com.github.charleslzq.faceengine.view.FotoCameraOperatorSource
import com.github.charleslzq.faceengine.view.UVCCameraOperatorSource
import java.util.concurrent.atomic.AtomicBoolean

class CameraSettingManager {
    private val updated = AtomicBoolean(false)
    private var cachedSetting = doLoadSetting()
    private val requestMap = mutableMapOf<CameraPreviewOperator, CameraPreviewRequest>()

    fun loadSetting() = if (updated.get()) {
        doLoadSetting().also {
            cachedSetting = it
        }
    } else {
        cachedSetting
    }

    fun loadRequest(camera: CameraPreviewOperator): CameraPreviewRequest = requestMap.getOrDefault(camera, when (camera) {
        is FotoCameraOperatorSource.FotoCameraPreviewOperator -> FotoCameraPreviewRequest()
        is UVCCameraOperatorSource.UVCCameraOperator -> UvcCameraPreviewRequest()
        else -> throw IllegalArgumentException("Unsupported camera operator")
    })

    fun configFor(camera: CameraPreviewOperator, config: () -> CameraPreviewRequest) {
        requestMap[camera] = config()
    }

    private fun doLoadSetting() = CameraSetting(
            CameraPreviewConfiguration(),
            emptyList()
    )
}