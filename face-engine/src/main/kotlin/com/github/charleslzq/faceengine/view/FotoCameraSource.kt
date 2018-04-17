package com.github.charleslzq.faceengine.view

import android.content.Context
import android.util.Log
import io.fotoapparat.Fotoapparat
import io.fotoapparat.FotoapparatBuilder
import io.fotoapparat.characteristic.LensPosition
import io.fotoapparat.log.logcat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.result.transformer.originalResolution
import io.fotoapparat.selector.back
import io.fotoapparat.selector.firstAvailable
import io.fotoapparat.selector.front
import io.fotoapparat.selector.single
import io.fotoapparat.view.CameraView
import io.reactivex.Scheduler
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun Frame.toPreviewFrame(seq: Int? = null) = CameraPreview.PreviewFrame(size, image, rotation, seq)

class FotoCameraSource(context: Context, cameraView: CameraView, parent: CameraSource? = null) : SeletableCameraSource() {
    private val frameProcessor = FrameToObservableProcessor()
    private val fotoapparat by lazy {
        Fotoapparat.with(context)
                .apply { setup(this, cameraView) }
                .build()
    }
    private val cameraOperators = listOf(LensPosition.Front, LensPosition.Back)
            .filter { fotoapparat.isAvailable(single(it)) }
            .map { FotoCameraPreviewOperator(it::class.java.simpleName, fotoapparat, cameraView, InternalCamera.fromLensSelector(it)) }

    override fun getCameras() = cameraOperators

    override fun onSelected(operator: CameraPreviewOperator?) {
        (operator as? FotoCameraPreviewOperator)?.switchToThis()
    }

    override fun onPreviewFrame(
            scheduler: Scheduler,
            processor: (CameraPreview.PreviewFrame) -> Unit
    ) = frameProcessor.publisher.observeOn(scheduler).subscribe(processor)

    override fun onPreviewFrame(
            scheduler: Scheduler,
            frameConsumer: CameraPreview.FrameConsumer
    ) = frameProcessor.publisher.observeOn(scheduler).subscribe { frameConsumer.accept(it) }

    private fun setup(fotoapparatBuilder: FotoapparatBuilder, cameraView: CameraView) {
        fotoapparatBuilder
                .lensPosition(
                        firstAvailable(
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

    class FotoCameraPreviewOperator(
            override val id: String,
            private val fotoapparat: Fotoapparat,
            private val cameraView: CameraView,
            private val camera: InternalCamera
    ) : CameraPreviewOperator {
        private val _isPreviewing = AtomicBoolean(false)

        override fun startPreview() {
            if (_isPreviewing.compareAndSet(false, true)) {
                fotoapparat.start()
            }
        }

        override fun stopPreview() {
            if (_isPreviewing.compareAndSet(true, false)) {
                fotoapparat.stop()
            }
        }

        override fun isPreviewing() = _isPreviewing.get()

        override fun takePicture() = fotoapparat.takePicture().toBitmap(originalResolution()).transform { it.bitmap }.await()

        fun switchToThis() {
            fotoapparat.switchTo(camera.lensPosition, camera.configuration)
        }
    }

    class FrameToObservableProcessor : FrameProcessor {
        val publisher = PublishSubject.create<CameraPreview.PreviewFrame>()
        private val count = AtomicInteger(0)

        override fun process(frame: Frame) {
            count.getAndIncrement().let {
                Log.i(TAG, "Publishing frame with seq $it")
                publisher.onNext(frame.toPreviewFrame(it))
            }
            if (count.get() > 1000) {
                count.set(0)
            }
        }
    }

    companion object {
        const val TAG = "FotoCameraSource"
    }
}