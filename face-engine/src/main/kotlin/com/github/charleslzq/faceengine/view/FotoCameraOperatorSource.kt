package com.github.charleslzq.faceengine.view

import android.content.Context
import android.util.Log
import com.github.charleslzq.faceengine.view.config.*
import io.fotoapparat.Fotoapparat
import io.fotoapparat.FotoapparatBuilder
import io.fotoapparat.characteristic.LensPosition
import io.fotoapparat.log.logcat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.result.adapter.rxjava2.toSingle
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun Frame.toPreviewFrame(source: String, seq: Int) = SourceAwarePreviewFrame(source, seq, size, image, rotation)

class FotoCameraOperatorSource(
        context: Context,
        cameraView: CameraView,
        submit: (Frame, (Frame) -> SourceAwarePreviewFrame) -> Unit
) : CameraOperatorSource() {
    override val id: String = "FOTO"
    private val frameProcessor = FrameToObservableProcessor(submit)
    private val fotoapparat by lazy {
        Fotoapparat.with(context)
                .apply { setup(this, cameraView) }
                .build()
    }
    override val cameras = listOf(LensPosition.Front, LensPosition.Back)
            .filter { fotoapparat.isAvailable(single(it)) }
            .map { FotoCameraPreviewOperator(it::class.java.simpleName, this, fotoapparat, InternalCamera.fromLensSelector(it), frameProcessor) }

    private fun setup(fotoapparatBuilder: FotoapparatBuilder, cameraView: CameraView) {
        fotoapparatBuilder
                .lensPosition(
                        firstAvailable(
                                front(),
                                back()
                        )
                )
                .previewScaleType(ScaleType.CenterInside)
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
            internal var camera: InternalCamera,
            private val frameProcessor: FrameToObservableProcessor
    ) : CameraPreviewOperator {
        private val _isPreviewing = AtomicBoolean(false)

        override fun startPreview(request: CameraPreviewRequest) {
            if (_isPreviewing.compareAndSet(false, true)) {
                fotoapparat.start()
                fotoapparat.switchTo(camera.lensPosition, camera.configuration.copy(
                        frameProcessor = {
                            frameProcessor.process(it)
                        },
                        previewResolution = request.resolutionSelector.instance
                ))
            }
        }

        override fun stopPreview() {
            if (_isPreviewing.compareAndSet(true, false)) {
                fotoapparat.stop()
            }
        }

        override fun isPreviewing() = _isPreviewing.get()

        override fun getCapabilities(): Single<CameraCapabilities> = fotoapparat.getCapabilities().transform {
            FotoCameraCapabilities(it.previewResolutions.toList()) as CameraCapabilities
        }.toSingle().subscribeOn(Schedulers.io())

        override fun getCurrentParameters(): Single<CameraParameters> = fotoapparat.getCurrentParameters().transform {
            FotoCameraParameters(it.previewResolution) as CameraParameters
        }.toSingle().subscribeOn(Schedulers.io())
    }

    class FrameToObservableProcessor(private val submit: (Frame, (Frame) -> SourceAwarePreviewFrame) -> Unit) : FrameProcessor {
        private val count = AtomicInteger(0)

        override fun process(frame: Frame) {
            submit(frame) {
                count.getAndIncrement().let {
                    frame.toPreviewFrame("FOTO", it)
                }
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