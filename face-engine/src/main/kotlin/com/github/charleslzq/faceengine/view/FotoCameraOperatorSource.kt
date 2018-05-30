package com.github.charleslzq.faceengine.view

import android.content.Context
import android.util.Log
import com.github.charleslzq.faceengine.support.faceEngineTaskExecutor
import io.fotoapparat.Fotoapparat
import io.fotoapparat.FotoapparatBuilder
import io.fotoapparat.characteristic.LensPosition
import io.fotoapparat.log.logcat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import io.reactivex.Scheduler
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun Frame.toPreviewFrame(source: String, seq: Int? = null) = CameraPreview.PreviewFrame(source, size, image, rotation, seq)

class FotoCameraOperatorSource(
        context: Context,
        cameraView: CameraView,
        override val sampleInterval: Long,
        override val switchToThis: (String) -> Unit
) : CameraOperatorSource() {
    override val id: String = "FOTO"
    override var selected = false
    private val frameProcessor = FrameToObservableProcessor()
    private val fotoapparat by lazy {
        Fotoapparat.with(context)
                .apply { setup(this, cameraView) }
                .build()
    }
    private val cameraOperators = listOf(LensPosition.Front, LensPosition.Back)
            .filter { fotoapparat.isAvailable(single(it)) }
            .map { FotoCameraPreviewOperator(it::class.java.simpleName, fotoapparat, InternalCamera.fromLensSelector(it), frameProcessor) }

    override fun getCameras() = cameraOperators

    override fun onSelected(operator: CameraPreviewOperator?) {
        (operator as? FotoCameraPreviewOperator)?.switchToThis()
    }

    override fun onPreviewFrame(
            scheduler: Scheduler,
            processor: (CameraPreview.PreviewFrame) -> Unit
    ) = frameProcessor.publisher.observeOn(scheduler).sample(sampleInterval, TimeUnit.MILLISECONDS).subscribe(processor)

    override fun onPreviewFrame(
            scheduler: Scheduler,
            frameConsumer: CameraPreview.FrameConsumer
    ) = frameProcessor.publisher.observeOn(scheduler).sample(sampleInterval, TimeUnit.MILLISECONDS).subscribe { frameConsumer.accept(it) }

    override fun close() {
        selectedCamera?.stopPreview()
    }

    private fun setup(fotoapparatBuilder: FotoapparatBuilder, cameraView: CameraView) {
        fotoapparatBuilder
                .lensPosition(
                        firstAvailable(
                                front(),
                                back()
                        )
                )
                .previewScaleType(ScaleType.CenterInside)
                .photoResolution(lowestResolution())
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
            private val camera: InternalCamera,
            private val frameProcessor: FrameToObservableProcessor
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
                faceEngineTaskExecutor.cancelTasks(id)
            }
        }

        override fun isPreviewing() = _isPreviewing.get()

        fun switchToThis() {
            fotoapparat.switchTo(camera.lensPosition, camera.configuration.copy(
                    frameProcessor = {
                        frameProcessor.process(it)
                    }
            ))
        }
    }

    class FrameToObservableProcessor : FrameProcessor {
        val publisher = PublishSubject.create<CameraPreview.PreviewFrame>()
        private val count = AtomicInteger(0)

        override fun process(frame: Frame) {
            count.getAndIncrement().let {
                publisher.onNext(frame.toPreviewFrame("FOTO", it))
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