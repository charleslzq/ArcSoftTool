package com.github.charleslzq.faceengine.view

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.github.charleslzq.faceengine.core.TrackedFace
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.view.CameraView
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable

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

    private val cameraSources = listOf(
            UVCCameraSource(context, cameraView),
            FotoCameraSource(context, cameraView)
    )
    override val selectedCamera: CameraPreviewOperator?
        get() = sourceSelector(cameraSources)?.selectedCamera
    var sourceSelector: (Iterable<SeletableCameraSource>) -> SeletableCameraSource? = { it.firstOrNull() }
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

    override fun getCameras() = cameraSources.flatMap { it.getCameras() }

    fun updateTrackFaces(faces: Collection<TrackedFace>) = trackView.resetRects(faces)

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

    fun getCurrentSource() = sourceSelector(cameraSources)

    class CompositeDisposable(private val disposables: List<Disposable>) : Disposable {
        override fun isDisposed() = disposables.all { it.isDisposed }

        override fun dispose() = disposables.forEach { it.dispose() }
    }

    companion object {
        const val TAG = "FaceDetectView"
    }
}