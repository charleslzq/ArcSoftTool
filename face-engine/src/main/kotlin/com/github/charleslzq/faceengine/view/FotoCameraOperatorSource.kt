package com.github.charleslzq.faceengine.view

import android.content.Context
import android.util.Log
import io.fotoapparat.Fotoapparat
import io.fotoapparat.FotoapparatBuilder
import io.fotoapparat.characteristic.LensPosition
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun Frame.toPreviewFrame(source: String, seq: Int) = SourceAwarePreviewFrame(source, seq, size, image, rotation)

class FotoCameraOperatorSource(
        context: Context,
        cameraView: CameraView,
        override var cameraPreviewConfiguration: CameraPreviewConfiguration
) : CameraOperatorSource() {
    override val id: String = "FOTO"
    private val frameProcessor = FrameToObservableProcessor(cameraPreviewConfiguration)
    private val fotoapparat by lazy {
        Fotoapparat.with(context)
                .apply { setup(this, cameraView) }
                .build()
    }
    override val cameras = listOf(LensPosition.Front, LensPosition.Back)
            .filter { fotoapparat.isAvailable(single(it)) }
            .map { FotoCameraPreviewOperator(it::class.java.simpleName, this, fotoapparat, InternalCamera.fromLensSelector(it, cameraPreviewConfiguration), frameProcessor) }

    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {
        super.applyConfiguration(cameraPreviewConfiguration)
        frameProcessor.cameraPreviewConfiguration = cameraPreviewConfiguration
        fotoapparat.updateConfiguration(CameraConfiguration.builder()
                .previewResolution(cameraPreviewConfiguration.previewResolution)
                .build())
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
                .previewResolution(cameraPreviewConfiguration.previewResolution)
                .previewFpsRange(highestFixedFps())
                .frameProcessor(frameProcessor)
                .into(cameraView)
                .logger(logcat())
                .cameraErrorCallback {
                    Log.e(TAG, "Error with fotoapparat", it)
                }
    }

    override fun close() {
        cameras.forEach { it.stopPreview() }
    }

    class FotoCameraPreviewOperator(
            override val id: String,
            override val source: CameraOperatorSource,
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
            }
        }

        override fun isPreviewing() = _isPreviewing.get()

        override fun onSelected() {
            fotoapparat.switchTo(camera.lensPosition, camera.configuration.copy(
                    frameProcessor = {
                        frameProcessor.process(it)
                    }
            ))
        }
    }

    class FrameToObservableProcessor(var cameraPreviewConfiguration: CameraPreviewConfiguration) : FrameProcessor {
        private val count = AtomicInteger(0)

        override fun process(frame: Frame) {
            cameraPreviewConfiguration.frameTaskRunner.transformAndSubmit(frame) {
                count.getAndIncrement().let {
                    frame.toPreviewFrame("FOTO", it)
                }
            }
        }
    }

    companion object {
        const val TAG = "FotoCameraSource"
    }
}