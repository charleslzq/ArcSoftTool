package com.github.charleslzq.faceengine.view

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.view.task.RxFrameTaskRunner
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.view.CameraView
import java.util.concurrent.TimeUnit

/**
 * Created by charleslzq on 18-3-8.
 */
val CameraView.textureView
    get() = getChildAt(0) as TextureView

class FaceDetectView
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0) :
        FrameLayout(context, attributeSet, defStyle), CameraSource {
    private var cameraPreviewConfiguration: CameraPreviewConfiguration = CameraPreviewConfiguration.from(context, attributeSet, defStyle)
    private val frameTaskRunner = RxFrameTaskRunner(cameraPreviewConfiguration.sampleInterval)
    private val cameraView = CameraView(context, attributeSet, defStyle).also {
        it.setScaleType(ScaleType.CenterInside)
        addView(it)
    }
    private val trackView = TrackView(context, attributeSet, defStyle).also {
        it.applyConfiguration(cameraPreviewConfiguration)
        addView(it)
    }
    private val cameraSources = listOf(
            UVCCameraOperatorSource(context, cameraView, { frameTaskRunner.consume(it) }, cameraPreviewConfiguration),
            FotoCameraOperatorSource(context, cameraView, { frameTaskRunner.consume(it) }, cameraPreviewConfiguration)
    )
    override val cameras: List<CameraPreviewOperator>
        get() = cameraSources.flatMap { it.cameras }
    var selectCamera: (Iterable<CameraPreviewOperator>) -> CameraPreviewOperator? = { null }
        set(value) {
            val oldCamera = field(cameras)
            val newCamera = value(cameras)
            if (oldCamera != newCamera) {
                oldCamera?.stopPreview()
                field = value
                newCamera?.onSelected()
                newCamera?.startPreview()
            }
        }
    val selectedCamera: CameraPreviewOperator?
        get() = selectCamera(cameras)
    val selectedSource: CameraOperatorSource?
        get() = selectedCamera?.source


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        cameraView.layout(left, top, right, bottom)
        trackView.layout(
                cameraView.textureView.left,
                cameraView.textureView.top,
                cameraView.textureView.right,
                cameraView.textureView.bottom
        )
    }

    @JvmOverloads
    fun onPreview(
            timeout: Long = 2000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            processor: (SourceAwarePreviewFrame) -> Unit
    ) = frameTaskRunner.onPreviewFrame(timeout, timeUnit, processor)

    @JvmOverloads
    fun onPreview(
            timeout: Long = 2000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            frameConsumer: FrameConsumer
    ) = frameTaskRunner.onPreviewFrame(timeout, timeUnit, {
        frameConsumer.accept(it)
    })

    fun updateTrackFaces(faces: Collection<TrackedFace>) {
        if (cameraPreviewConfiguration.showRect) {
            trackView.resetRects(faces)
        }
    }

    fun selectNext() {
        selectedCamera?.let {
            selectAt(cameras.indexOf(it) + 1)
        }
    }

    fun selectAt(index: Int) {
        selectCamera = {
            it.elementAtOrNull(index % it.count())
        }
    }

    override fun open() {
        cameraSources.forEach { it.open() }
        if (selectedCamera == null || !selectedCamera!!.isPreviewing()) {
            selectCamera = { it.firstOrNull() }
        }
    }

    fun pause() {
        selectedCamera?.stopPreview()
    }

    override fun close() {
        cameraSources.forEach {
            it.close()
        }
        frameTaskRunner.cancelAll()
    }

    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {
        updateConfiguration { cameraPreviewConfiguration }
    }

    private fun updateConfiguration(generator: CameraPreviewConfiguration.() -> CameraPreviewConfiguration) {
        cameraPreviewConfiguration = generator(cameraPreviewConfiguration)
        cameraSources.forEach { it.applyConfiguration(cameraPreviewConfiguration) }
        trackView.applyConfiguration(cameraPreviewConfiguration)
    }

    companion object {
        const val TAG = "FaceDetectView"
    }
}