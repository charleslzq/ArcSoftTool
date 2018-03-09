package com.github.charleslzq.faceengine.core.kotlin.support

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import io.fotoapparat.Fotoapparat
import io.fotoapparat.FotoapparatBuilder
import io.fotoapparat.log.logcat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.external
import io.fotoapparat.selector.firstAvailable
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by charleslzq on 18-3-8.
 */
open class FaceDetectView
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) :
    FrameLayout(context, attributeSet, defStyle) {
    private var _isRunning = AtomicBoolean(false)
    val isRunning: Boolean
        get() = _isRunning.get()

    protected val cameraView = CameraView(context, attributeSet, defStyle).also { addView(it) }
    protected val frameProcessor = FrameToObservableProcessor()
    protected val fotoapparat by lazy {
        Fotoapparat.with(context)
            .apply { setup(this) }
            .build()
    }

    open fun setup(fotoapparatBuilder: FotoapparatBuilder) {
        fotoapparatBuilder
            .lensPosition(
                firstAvailable(
                    front(),
                    external()
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

    fun start() {
        if (_isRunning.compareAndSet(false, true)) {
            fotoapparat.start()
        }
    }

    fun stop() {
        if (_isRunning.compareAndSet(true, false)) {
            fotoapparat.stop()
        }
    }

    @JvmOverloads
    fun onPreviewFrame(
        scheduler: Scheduler = AndroidSchedulers.mainThread(),
        processor: (Frame) -> Unit
    ) = frameProcessor.publisher.observeOn(scheduler).subscribe(processor)

    @JvmOverloads
    fun onPreviewFrame(
        scheduler: Scheduler = AndroidSchedulers.mainThread(),
        frameConsumer: FrameConsumer
    ) = frameProcessor.publisher.observeOn(scheduler).subscribe { frameConsumer.accept(it) }

    class FrameToObservableProcessor : FrameProcessor {
        val publisher = PublishSubject.create<Frame>()

        override fun process(frame: Frame) {
            publisher.onNext(frame)
        }
    }

    @FunctionalInterface
    interface FrameConsumer {
        fun accept(frame: Frame)
    }

    companion object {
        const val TAG = "FaceDetectView"
    }
}