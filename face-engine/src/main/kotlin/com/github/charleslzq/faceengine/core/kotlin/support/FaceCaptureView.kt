package com.github.charleslzq.faceengine.core.kotlin.support

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import com.github.charleslzq.faceengine.core.R
import com.google.android.cameraview.CameraView
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by charleslzq on 18-3-6.
 */
fun TypedArray.use(process: TypedArray.() -> Unit) = apply {
    process(this)
    recycle()
}

class FaceCaptureView(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) :
    CameraView(context, attributeSet, defStyle) {
    private var pause = AtomicBoolean(true)
    private var disposable: Disposable? = null
    private val observablePictureCallback = ObservablePictureCallback()
    private var autoStart = true
    var interval: Int = DEFAULT_INTERVAL.toInt()

    constructor(context: Context, attributeSet: AttributeSet? = null) : this(
        context,
        attributeSet,
        0
    )

    constructor(context: Context) : this(context, null, 0)

    init {
        addCallback(observablePictureCallback)
        if (attributeSet != null) {
            context.obtainStyledAttributes(attributeSet, R.styleable.FaceCaptureView, defStyle, 0)
                .use {
                    interval = getInt(
                        R.styleable.FaceCaptureView_interval,
                        DEFAULT_INTERVAL.toInt()
                    )
                    autoStart = getBoolean(
                        R.styleable.FaceCaptureView_autoTakePicture,
                        true
                    ) && interval > 0
                }
        }
    }

    fun schedule(runner: (FaceCaptureView) -> Unit) {
        disposable?.dispose()
        pauseCapture()
        disposable = runOnUI { runner.invoke(this) }
        resumeCapture()
    }

    fun period(newInterval: Long, runner: (FaceCaptureView) -> Unit) {
        disposable?.dispose()
        pauseCapture()
        disposable = runOnUIWithInterval(newInterval) { runner.invoke(this) }
        resumeCapture()
    }

    fun onNewPicture(
        scheduler: Scheduler = AndroidSchedulers.mainThread(),
        processor: (Pair<Int, Bitmap>) -> Unit
    ): Disposable {
        return observablePictureCallback.publisher.observeOn(scheduler).subscribe(processor)
    }

    fun isCapturing() = !pause.get()

    fun pauseCapture() = pause.compareAndSet(false, true)

    fun resumeCapture() = pause.compareAndSet(true, false)

    fun resetCount() {
        observablePictureCallback.count = 0
    }

    override fun takePicture() {
        if (isCapturing() && isCameraOpened) {
            super.takePicture()
        }
    }

    override fun stop() {
        disposable?.dispose()
        pauseCapture()
        Thread.sleep(200)
        super.stop()
    }

    class ObservablePictureCallback : CameraView.Callback() {
        val publisher = PublishSubject.create<Pair<Int, Bitmap>>()
        var count = 0

        override fun onCameraOpened(cameraView: CameraView?) {
            super.onCameraOpened(cameraView)
            (cameraView as? FaceCaptureView)?.apply {
                if (autoStart) {
                    period(interval.toLong()) {
                        takePicture()
                    }
                }
            }
        }

        override fun onPictureTaken(cameraView: CameraView, data: ByteArray) {
            super.onPictureTaken(cameraView, data)
            if (cameraView is FaceCaptureView) {
                publisher.onNext(callOnCompute {
                    ++count to BitmapFactory.decodeByteArray(
                        data,
                        0,
                        data.size
                    )
                })
            }
        }
    }

    companion object {
        const val DEFAULT_INTERVAL = 1000L
    }
}