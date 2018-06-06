package com.github.charleslzq.faceengine.view

import android.content.res.TypedArray
import android.graphics.Color
import com.github.charleslzq.faceengine.core.R
import com.github.charleslzq.faceengine.view.task.CoroutineFrameTaskRunner
import com.github.charleslzq.faceengine.view.task.FrameTaskRunner
import com.github.charleslzq.faceengine.view.task.RxFrameTaskRunner
import io.fotoapparat.parameter.Resolution

fun <R> TypedArray.extract(setup: TypedArray.() -> R): R {
    val result = setup(this)
    recycle()
    return result
}

data class CameraPreviewConfiguration(
        val previewResolution: (Iterable<Resolution>) -> Resolution? = PreviewResolution.LOWEST.selector,
        val autoSwitchToNewDevice: Boolean = true,
        val showRect: Boolean = true,
        val rectColor: Int = Color.RED,
        val rectWidth: Float = 1f,
        val frameTaskRunner: FrameTaskRunner = CoroutineFrameTaskRunner(DEFAULT_ENABLE_SAMPLE, DEFAULT_SAMPLE_INTERVAL.toLong())
) {
    fun withNewPreviewResolution(selector: (Iterable<Resolution>) -> Resolution?) = copy(
            previewResolution = selector
    )

    fun withNewPreviewResolution(selector: Selector<Resolution>) = copy(
            previewResolution = { selector.select(it) }
    )

    fun withNewAutoSwitchToNewDevice(autoSwitchToNewDevice: Boolean) = copy(
            autoSwitchToNewDevice = autoSwitchToNewDevice
    )

    fun withNewShowRect(showRect: Boolean) = copy(
            showRect = showRect
    )

    fun withNewRectColor(rectColor: Int) = copy(
            rectColor = rectColor
    )

    fun withNewRectWidth(rectWidth: Float) = copy(
            rectWidth = rectWidth
    )

    fun withNewFrameTaskFunner(frameTaskRunner: FrameTaskRunner) = copy(
            frameTaskRunner = frameTaskRunner
    )

    companion object {
        const val DEFAULT_RESOLUTION_ID = 0
        const val DEFAULT_TASK_RUNNER_ID = 0
        const val DEFAULT_ENABLE_SAMPLE = false
        const val DEFAULT_SAMPLE_INTERVAL = 200
        const val DEFAULT_TRACK = true
        const val DEFAULT_AUTO_SWITCH = true
        const val DEFAULT_COLOR = Color.RED
        const val DEFAULT_WIDTH = 1f

        fun from(typedArray: TypedArray) = CameraPreviewConfiguration(
                previewResolution = PreviewResolution.fromAttrs(typedArray.getInt(R.styleable.FaceDetectView_previewResolution, DEFAULT_RESOLUTION_ID)).selector,
                showRect = typedArray.getBoolean(R.styleable.FaceDetectView_showTrackRect, DEFAULT_TRACK),
                autoSwitchToNewDevice = typedArray.getBoolean(R.styleable.FaceDetectView_autoSwitchToNewDevice, DEFAULT_AUTO_SWITCH),
                rectColor = typedArray.getColor(R.styleable.FaceDetectView_rectColor, DEFAULT_COLOR),
                rectWidth = typedArray.getDimension(R.styleable.FaceDetectView_rectWidth, DEFAULT_WIDTH),
                frameTaskRunner = detectRunner(typedArray)
        )

        private fun detectRunner(typedArray: TypedArray): FrameTaskRunner {
            val enableSample = typedArray.getBoolean(R.styleable.FaceDetectView_enableSample, DEFAULT_ENABLE_SAMPLE)
            val sampleInterval = typedArray.getInteger(R.styleable.FaceDetectView_sampleInterval, DEFAULT_SAMPLE_INTERVAL).toLong()
            return when (TaskRunner.fromAttrs(typedArray.getInt(R.styleable.FaceDetectView_taskRunner, DEFAULT_TASK_RUNNER_ID))) {
                TaskRunner.COROUTINE -> CoroutineFrameTaskRunner(enableSample, sampleInterval)
                TaskRunner.RX -> RxFrameTaskRunner(enableSample, sampleInterval)
            }
        }
    }

    enum class TaskRunner {
        COROUTINE,
        RX;

        companion object {
            fun fromAttrs(id: Int) = TaskRunner.values()[id]
        }
    }

    enum class PreviewResolution(val selector: (Iterable<Resolution>) -> Resolution?) {
        HIGHEST({ it.firstOrNull() }),
        LOWEST({ it.lastOrNull() });

        companion object {
            fun fromAttrs(id: Int) = PreviewResolution.values()[id]
        }
    }
}