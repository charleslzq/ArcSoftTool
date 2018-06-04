package com.github.charleslzq.faceengine.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.support.annotation.AttrRes
import android.util.AttributeSet
import com.github.charleslzq.faceengine.core.R
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
    fun startPreview()
    fun stopPreview()
    fun isPreviewing(): Boolean
    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {}
}

interface CameraSource : CameraPreviewConfigurable, AutoCloseable {
    val selectedCamera: CameraPreviewOperator?
    val cameras: List<CameraPreviewOperator>
    fun start() {}
}

abstract class CameraOperatorSource : CameraSource {
    var operatorSelector: (Iterable<CameraPreviewOperator>) -> CameraPreviewOperator? = { it.firstOrNull() }
        set(value) {
            val oldSelection = field(cameras)
            val newSelection = value(cameras)
            if (oldSelection != newSelection) {
                if (selected) {
                    oldSelection?.stopPreview()
                }
                field = value
                if (selected) {
                    onSelected(newSelection)
                    newSelection?.startPreview()
                }
            }
        }
    override val selectedCamera: CameraPreviewOperator?
        get() = operatorSelector(cameras)
    abstract var cameraPreviewConfiguration: CameraPreviewConfiguration
    abstract var selected: Boolean
    abstract val id: String
    abstract val switchToThis: (String) -> Unit

    abstract fun onSelected(operator: CameraPreviewOperator?)

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
        val rectColor: Int = Color.RED,
        val rectWidth: Float = 1f
) {
    companion object {
        const val DEFAULT_RESOLUTION_ID = 1
        const val DEFAULT_INTERVAL = 500
        const val DEFAULT_TRACK = true
        const val DEFAULT_COLOR = Color.RED
        const val DEFAULT_WIDTH = 1f

        fun from(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0): CameraPreviewConfiguration {
            return attributeSet?.let { context.obtainStyledAttributes(it, R.styleable.FaceDetectView) }?.extract {
                CameraPreviewConfiguration(
                        previewResolution = PreviewResolution.fromAttrs(getInt(R.styleable.FaceDetectView_previewResolution, DEFAULT_RESOLUTION_ID)).selector,
                        sampleInterval = getInteger(R.styleable.FaceDetectView_sampleInterval, DEFAULT_INTERVAL).toLong(),
                        showRect = getBoolean(R.styleable.FaceDetectView_showTrackRect, DEFAULT_TRACK),
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