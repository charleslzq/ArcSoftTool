package com.github.charleslzq.faceengine.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import com.github.charleslzq.faceengine.core.R
import com.github.charleslzq.faceengine.view.task.FrameTaskRunner
import com.github.charleslzq.faceengine.view.task.RxFrameTaskRunner
import io.fotoapparat.parameter.Resolution

sealed class PreviewFrame(
        val size: Resolution,
        val image: ByteArray,
        val rotation: Int
)

class SimplePreviewFrame(
        size: Resolution,
        image: ByteArray,
        rotation: Int
) : PreviewFrame(size, image, rotation)

class SourceAwarePreviewFrame(
        val source: String,
        val sequence: Int,
        size: Resolution,
        image: ByteArray,
        rotation: Int
) : PreviewFrame(size, image, rotation)

@FunctionalInterface
interface FrameConsumer {
    fun accept(previewFrame: SourceAwarePreviewFrame)
}

@FunctionalInterface
interface Selector<T> {
    fun select(choices: Iterable<T>): T?
}

interface CameraPreviewConfigurable {
    fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration)
}

interface CameraPreviewOperator : CameraPreviewConfigurable {
    val id: String
    val source: CameraOperatorSource
    fun startPreview()
    fun stopPreview()
    fun isPreviewing(): Boolean
    fun onSelected() {}
    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {}
}

interface CameraSource : CameraPreviewConfigurable, AutoCloseable {
    val cameras: List<CameraPreviewOperator>
    fun open() {}
    override fun close() {}
}

abstract class CameraOperatorSource : CameraSource {
    abstract var cameraPreviewConfiguration: CameraPreviewConfiguration
    abstract val id: String

    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {
        this.cameraPreviewConfiguration = cameraPreviewConfiguration
    }
}

fun <R> TypedArray.extract(setup: TypedArray.() -> R): R {
    val result = setup(this)
    recycle()
    return result
}

class CameraPreviewConfiguration(
        val previewResolution: (Iterable<Resolution>) -> Resolution? = PreviewResolution.LOWEST.selector,
        val sampleInterval: Long = 500,
        val showRect: Boolean = true,
        val autoSwitchToNewDevice: Boolean = true,
        val rectColor: Int = Color.RED,
        val rectWidth: Float = 1f,
        val frameTaskRunner: FrameTaskRunner = RxFrameTaskRunner(sampleInterval)
) {
    companion object {
        const val DEFAULT_RESOLUTION_ID = 1
        const val DEFAULT_INTERVAL = 500
        const val DEFAULT_TRACK = true
        const val DEFAULT_AUTO_SWITCH = true
        const val DEFAULT_COLOR = Color.RED
        const val DEFAULT_WIDTH = 1f

        fun from(context: Context, attributeSet: AttributeSet? = null): CameraPreviewConfiguration {
            return attributeSet?.let { context.obtainStyledAttributes(it, R.styleable.FaceDetectView) }?.extract {
                CameraPreviewConfiguration(
                        previewResolution = PreviewResolution.fromAttrs(getInt(R.styleable.FaceDetectView_previewResolution, DEFAULT_RESOLUTION_ID)).selector,
                        sampleInterval = getInteger(R.styleable.FaceDetectView_sampleInterval, DEFAULT_INTERVAL).toLong(),
                        showRect = getBoolean(R.styleable.FaceDetectView_showTrackRect, DEFAULT_TRACK),
                        autoSwitchToNewDevice = getBoolean(R.styleable.FaceDetectView_autoSwitchToNewDevice, DEFAULT_AUTO_SWITCH),
                        rectColor = getColor(R.styleable.FaceDetectView_rectColor, DEFAULT_COLOR),
                        rectWidth = getDimension(R.styleable.FaceDetectView_rectWidth, DEFAULT_WIDTH)
                )
            } ?: CameraPreviewConfiguration()
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