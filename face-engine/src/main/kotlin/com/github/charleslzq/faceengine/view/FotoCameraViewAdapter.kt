package com.github.charleslzq.faceengine.view

import android.app.Activity
import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.github.charleslzq.faceengine.core.TrackedFace
import io.fotoapparat.Fotoapparat
import io.fotoapparat.FotoapparatBuilder
import io.fotoapparat.log.logcat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.back
import io.fotoapparat.selector.external
import io.fotoapparat.selector.firstAvailable
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import io.reactivex.Scheduler
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean

fun Frame.toPreviewFrame() = CameraPreview.PreviewFrame(size, image, rotation)

internal class FotoCameraViewAdapter
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0) :
        FrameLayout(context, attributeSet, defStyle), CameraInterface {

    private val _isRunning = AtomicBoolean(false)

    private val cameraView = CameraView(context, attributeSet, defStyle).also { addView(it) }
    private val trackView = TrackView(context, attributeSet, defStyle).also { addView(it) }
    private val frameProcessor = FrameToObservableProcessor()
    private val fotoapparat by lazy {
        Fotoapparat.with(context)
                .apply { setup(this) }
                .build()
    }

    init {
        visibility = View.INVISIBLE
    }

    private fun setup(fotoapparatBuilder: FotoapparatBuilder) {
        fotoapparatBuilder
                .lensPosition(
                        firstAvailable(
                                external(),
                                front(),
                                back()
                        )
                )
                .previewScaleType(ScaleType.CenterInside)
                .frameProcessor(frameProcessor)
                .into(cameraView)
                .logger(logcat())
                .cameraErrorCallback {
                    Log.e(TAG, "Error with fotoapparat", it)
                }
    }

    override fun start(activity: Activity) {
        if (_isRunning.compareAndSet(false, true)) {
            fotoapparat.start()
            visibility = View.VISIBLE
        }
    }

    override fun pause() {
        if (_isRunning.compareAndSet(true, false)) {
            fotoapparat.stop()
            visibility = View.INVISIBLE
        }
    }

    override fun stop() {
        if (_isRunning.compareAndSet(true, false)) {
            fotoapparat.stop()
            visibility = View.INVISIBLE
        }
    }

    override fun isRunning() = _isRunning.get()

    override fun updateTrackFaces(faces: List<TrackedFace>) {
        if (trackView.track) {
            trackView.resetRects(faces)
        }
    }

    override fun hasAvailableCamera() = fotoapparat.isAvailable(front()) || fotoapparat.isAvailable(back())

    override fun onPreviewFrame(
            scheduler: Scheduler,
            processor: (CameraPreview.PreviewFrame) -> Unit
    ) = frameProcessor.publisher.observeOn(scheduler).subscribe(processor)

    override fun onPreviewFrame(
            scheduler: Scheduler,
            frameConsumer: CameraPreview.FrameConsumer
    ) = frameProcessor.publisher.observeOn(scheduler).subscribe { frameConsumer.accept(it) }

    class FrameToObservableProcessor : FrameProcessor {
        val publisher = PublishSubject.create<CameraPreview.PreviewFrame>()

        override fun process(frame: Frame) {
            publisher.onNext(frame.toPreviewFrame())
        }
    }

    companion object {
        const val TAG = "FotoCameraViewAdapter"
    }
}