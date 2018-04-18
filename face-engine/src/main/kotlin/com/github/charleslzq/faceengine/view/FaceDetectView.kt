package com.github.charleslzq.faceengine.view

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.github.charleslzq.faceengine.core.R
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.faceEngineTaskExecutor
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.view.CameraView
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
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
    private val cameraView = CameraView(context, attributeSet, defStyle).also {
        it.setScaleType(ScaleType.CenterInside)
        addView(it)
    }
    private val trackView = TrackView(context, attributeSet, defStyle).also { addView(it) }

    private val sampleInterval: Long = {
        attributeSet?.let { context.obtainStyledAttributes(it, R.styleable.FaceDetectView) }?.run {
            val interval = getInteger(R.styleable.FaceDetectView_sampleInterval, DEFAULT_INTERVAL)
            recycle()
            interval
        } ?: DEFAULT_INTERVAL
    }.invoke().toLong()
    private val cameraSources = listOf(
            UVCCameraOperatorSource(context, cameraView, sampleInterval),
            FotoCameraOperatorSource(context, cameraView, sampleInterval)
    )
    override val selectedCamera: CameraPreviewOperator?
        get() = operatorSourceSelector(cameraSources)?.selectedCamera
    var operatorSourceSelector: (Iterable<CameraOperatorSource>) -> CameraOperatorSource? = { it.firstOrNull { it.getCameras().isNotEmpty() } }
        set(value) {
            val oldSelection = field(cameraSources)
            val newSelection = value(cameraSources)
            if (oldSelection != newSelection) {
                oldSelection?.getCameras()?.forEach { it.stopPreview() }
                field = value
                newSelection?.selectedCamera?.startPreview()
            }
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        cameraView.layout(left, top, right, bottom)
        trackView.layout(
                cameraView.textureView.left,
                cameraView.textureView.top,
                cameraView.textureView.right,
                cameraView.textureView.bottom
        )
    }

    override fun onPreviewFrame(scheduler: Scheduler, processor: (CameraPreview.PreviewFrame) -> Unit) = CompositeDisposable(
            cameraSources.map {
                it.onPreviewFrame(scheduler, processor)
            }
    )

    override fun onPreviewFrame(scheduler: Scheduler, frameConsumer: CameraPreview.FrameConsumer) = CompositeDisposable(
            cameraSources.map {
                it.onPreviewFrame(scheduler, frameConsumer)
            }
    )

    @JvmOverloads
    fun onPreview(
            timeout: Long = 5000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            scheduler: Scheduler = Schedulers.computation(),
            processor: (CameraPreview.PreviewFrame) -> Unit
    ) = onPreviewFrame(scheduler) {
        faceEngineTaskExecutor.run {
            executeInTimeout(timeout, timeUnit) {
                processor(it)
            }
            logStatus()
        }
    }

    @JvmOverloads
    fun onPreview(
            timeout: Long = 1000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            scheduler: Scheduler = Schedulers.computation(),
            frameConsumer: CameraPreview.FrameConsumer
    ) = onPreviewFrame(scheduler) {
        faceEngineTaskExecutor.run {
            executeInTimeout(timeout, timeUnit) {
                frameConsumer.accept(it)
            }
            logStatus()
        }
    }

    override fun getCameras() = cameraSources.flatMap { it.getCameras() }

    fun updateTrackFaces(faces: Collection<TrackedFace>) {
        if (trackView.track) {
            trackView.resetRects(faces)
        }
    }

    override fun start() {
        cameraSources.forEach { it.start() }
        selectedCamera?.startPreview()
    }

    fun pause() {
        selectedCamera?.stopPreview()
    }

    override fun close() = cameraSources.forEach {
        it.close()
    }

    fun getCurrentSource() = operatorSourceSelector(cameraSources)

    class CompositeDisposable(private val disposables: List<Disposable>) : Disposable {
        override fun isDisposed() = disposables.all { it.isDisposed }

        override fun dispose() = disposables.forEach { it.dispose() }
    }

    companion object {
        const val TAG = "FaceDetectView"
        const val DEFAULT_INTERVAL = 100
    }
}