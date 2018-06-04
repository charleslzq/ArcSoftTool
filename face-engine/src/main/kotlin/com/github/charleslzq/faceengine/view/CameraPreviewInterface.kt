package com.github.charleslzq.faceengine.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.support.annotation.AttrRes
import android.util.AttributeSet
import com.github.charleslzq.faceengine.core.R
import io.fotoapparat.parameter.Resolution
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

interface CameraPreview : CameraPreviewConfigurable {
    fun onPreviewFrame(
            scheduler: Scheduler = Schedulers.computation(),
            processor: (PreviewFrame) -> Unit
    ): Disposable

    fun onPreviewFrame(
            scheduler: Scheduler = Schedulers.computation(),
            frameConsumer: FrameConsumer
    ): Disposable

    data class PreviewFrame(
            val source: String,
            val size: Resolution,
            val image: ByteArray,
            val rotation: Int,
            val sequence: Int? = null
    )

    @FunctionalInterface
    interface FrameConsumer {
        fun accept(previewFrame: PreviewFrame)
    }
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

interface CameraSource : CameraPreview, AutoCloseable {
    val selectedCamera: CameraPreviewOperator?
    fun start() {}
    fun getCameras(): List<CameraPreviewOperator>
}

abstract class CameraOperatorSource : CameraSource {
    var operatorSelector: (Iterable<CameraPreviewOperator>) -> CameraPreviewOperator? = { it.firstOrNull() }
        set(value) {
            val oldSelection = field(getCameras())
            val newSelection = value(getCameras())
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
        get() = operatorSelector(getCameras())
    abstract var cameraPreviewConfiguration: CameraPreviewConfiguration
    abstract var selected: Boolean
    abstract val id: String
    abstract val switchToThis: (String) -> Unit

    abstract fun onSelected(operator: CameraPreviewOperator?)

    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {
        this.cameraPreviewConfiguration = cameraPreviewConfiguration
    }

    fun selectCamera(selector: (Iterable<CameraPreviewOperator>) -> CameraPreviewOperator?) {
        operatorSelector = selector
    }

    fun selectCamera(selector: CameraSelector) {
        operatorSelector = { selector.select(it) }
    }

    @FunctionalInterface
    interface CameraSelector {
        fun select(choices: Iterable<CameraPreviewOperator>): CameraPreviewOperator?
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