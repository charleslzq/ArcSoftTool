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
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun Frame.toPreviewFrame(source: String, seq: Int) = SourceAwarePreviewFrame(source, seq, size, image, rotation)

class FotoCameraOperatorSource(
        context: Context,
        cameraView: CameraView,
        private val settingManager: CameraSettingManager,
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
            .map { FotoCameraPreviewOperator(it::class.java.simpleName, this, fotoapparat, InternalCamera.fromLensSelector(it), frameProcessor, settingManager) }

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
            private val frameProcessor: FrameToObservableProcessor,
            private val settingManager: CameraSettingManager
    ) : CameraPreviewOperator {
        private val _isPreviewing = AtomicBoolean(false)

        override fun startPreview() {
            if (_isPreviewing.compareAndSet(false, true)) {
                val request = settingManager.loadRequest(this)
                fotoapparat.start()
                fotoapparat.switchTo(camera.lensPosition, camera.configuration.copy(
                        frameProcessor = {
                            frameProcessor.process(it)
                        },
                        previewResolution = request.resolutionSelector.instance
                ))
            }
        }

        override fun updateConfig(request: CameraPreviewRequest) {
            stopPreview()
            settingManager.configFor(this, request)
            startPreview()
        }

        override fun stopPreview() {
            if (_isPreviewing.compareAndSet(true, false)) {
                fotoapparat.stop()
            }
        }

        override fun isPreviewing() = _isPreviewing.get()

        override fun getCapabilities(): CameraCapabilities = fotoapparat.getCapabilities().transform {
            FotoCameraCapabilities(it.previewResolutions.toList()) as CameraCapabilities
        }.toSingle().subscribeOn(Schedulers.io()).blockingGet()

        override fun getCurrentParameters(): CameraParameters = fotoapparat.getCurrentParameters().transform {
            FotoCameraParameters(it.previewResolution) as CameraParameters
        }.toSingle().subscribeOn(Schedulers.io()).blockingGet()
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