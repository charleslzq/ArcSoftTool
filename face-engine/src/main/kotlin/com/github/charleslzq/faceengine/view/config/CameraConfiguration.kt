package com.github.charleslzq.faceengine.view.config

import android.graphics.Color
import com.github.charleslzq.faceengine.view.task.FrameTaskRunner
import com.github.charleslzq.faceengine.view.task.TaskRunner

data class CameraPreviewConfiguration(
        val autoSwitchToNewDevice: Boolean = true,
        val showRect: Boolean = true,
        val rectColor: Int = Color.RED,
        val rectWidth: Float = 1f,
        val taskRunner: TaskRunner = TaskRunner.COROUTINE
) {
    val frameTaskRunner: FrameTaskRunner
        get() = taskRunner.instance
}

data class CameraSetting(
        val cameraPreviewConfiguration: CameraPreviewConfiguration,
        val cameraPreferences: List<CameraPreference>
)