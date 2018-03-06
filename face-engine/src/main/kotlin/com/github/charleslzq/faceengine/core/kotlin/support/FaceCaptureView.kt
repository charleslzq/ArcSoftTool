package com.github.charleslzq.faceengine.core.kotlin.support

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import com.github.charleslzq.faceengine.core.R
import com.google.android.cameraview.CameraView
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
    var interval: Int = DEFAULT_INTERVAL.toInt()
    var pause = AtomicBoolean(false)
    var count = 0
        private set
    val autoTakePictureCallback = AutoTakePictureCallback()

    constructor(context: Context, attributeSet: AttributeSet? = null) : this(
        context,
        attributeSet,
        0
    )

    constructor(context: Context) : this(context, null, 0)

    init {
        if (attributeSet != null) {
            context.obtainStyledAttributes(attributeSet, R.styleable.FaceCaptureView, defStyle, 0)
                .use {
                    interval = getInt(
                        R.styleable.FaceCaptureView_interval,
                        DEFAULT_INTERVAL.toInt()
                    )
                    if (getBoolean(R.styleable.FaceCaptureView_autoTakePicture, true)) {
                        addCallback(autoTakePictureCallback)
                    }
                }
        }
    }

    override fun stop() {
        pause.set(true)
        super.stop()
    }

    class AutoTakePictureCallback : CameraView.Callback() {
        private var disposable: Disposable? = null
        val publisher = PublishSubject.create<Bitmap>()

        override fun onCameraOpened(cameraView: CameraView) {
            super.onCameraOpened(cameraView)
            if (cameraView is FaceCaptureView) {
                disposable = runOnUIWithInterval(cameraView.interval.toLong()) {
                    if (!cameraView.pause.get()) {
                        cameraView.count++
                        cameraView.takePicture()
                    }
                }
            }
        }

        override fun onCameraClosed(cameraView: CameraView) {
            super.onCameraClosed(cameraView)
            disposable?.dispose()
        }

        override fun onPictureTaken(cameraView: CameraView, data: ByteArray) {
            super.onPictureTaken(cameraView, data)
            if (cameraView is FaceCaptureView) {
                publisher.onNext(BitmapFactory.decodeByteArray(data, 0, data.size))
            }
        }
    }

    companion object {
        const val DEFAULT_INTERVAL = 1000L
    }
}