package com.github.charleslzq.faceengine.view.config

import android.content.Context
import com.github.charleslzq.faceengine.view.CameraPreviewOperator

class CameraSettingManager(context: Context) {
    private val settingStore = SettingStore(context)

    fun loadSetting() = settingStore.load()

    fun savePreviewConfig(cameraPreviewConfiguration: CameraPreviewConfiguration) {
        val setting = loadSetting()
        settingStore.store(CameraSetting(cameraPreviewConfiguration, setting.cameraPreferences))
    }

    fun loadRequest(cameraPreviewOperator: CameraPreviewOperator): CameraPreviewRequest = loadSetting().findCamera(cameraPreviewOperator.source.id, cameraPreviewOperator.id)?.request
            ?: cameraPreviewOperator.getDefaultRequest()

    fun configFor(cameraPreviewOperator: CameraPreviewOperator, request: CameraPreviewRequest) {
        val setting = loadSetting()
        val target = setting.findCamera(cameraPreviewOperator.source.id, cameraPreviewOperator.id)

        settingStore.store(CameraSetting(
                setting.cameraPreviewConfiguration,
                setting.cameraPreferences.toMutableList().apply {
                    remove(target)
                    add(CameraPreference(cameraPreviewOperator.id, cameraPreviewOperator.source.id, request))
                }
        ))
    }
}