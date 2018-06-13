package com.github.charleslzq.faceengine.view.config

import android.graphics.Color
import com.github.charleslzq.faceengine.view.task.FrameTaskRunner
import com.github.charleslzq.faceengine.view.task.TaskRunner

interface Transformer<U, V> {
    fun transform(data: U): V
}

data class CameraPreviewConfiguration(
        var autoSwitchToNewDevice: Boolean = true,
        var showRect: Boolean = true,
        var rectColor: Int = Color.RED,
        var rectWidth: Float = 1f,
        var taskRunner: TaskRunner = TaskRunner.COROUTINE
) {
    val frameTaskRunner: FrameTaskRunner
        get() = taskRunner.instance
}

data class CameraSetting(
        val cameraPreviewConfiguration: CameraPreviewConfiguration,
        val cameraPreferences: List<CameraPreference>
) {
    fun findCamera(source: String, cameraId: String) = cameraPreferences.firstOrNull {
        it.cameraId == cameraId && it.source == source
    }
}