package com.github.charleslzq.faceengine.view.config

import android.content.Context

class CameraSettingManager(context: Context) {
    private val settingStore = SettingStore(context)

    fun loadSetting() = settingStore.load()

    fun loadRequest(source: String, cameraId: String, isFoto: Boolean): CameraPreviewRequest = loadSetting().findCamera(source, cameraId)?.request
            ?: CameraPreviewRequest.getDefaultRequest(isFoto)

    fun configFor(source: String, cameraId: String, request: CameraPreviewRequest) {
        val setting = loadSetting()
        val target = setting.findCamera(source, cameraId)

        settingStore.store(CameraSetting(
                setting.cameraPreviewConfiguration,
                setting.cameraPreferences.toMutableList().apply {
                    remove(target)
                    add(CameraPreference(cameraId, source, request))
                }
        ))
    }
}