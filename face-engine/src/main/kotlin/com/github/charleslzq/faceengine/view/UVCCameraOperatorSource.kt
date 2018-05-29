package com.github.charleslzq.faceengine.view

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import android.view.Surface
import android.widget.Toast
import com.github.charleslzq.faceengine.support.faceEngineTaskExecutor
import com.github.charleslzq.faceengine.support.runOnCompute
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.view.CameraView
import io.fotoapparat.view.Preview
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.subjects.PublishSubject
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class UVCCameraOperatorSource(
        context: Context,
        cameraView: CameraView,
        override val sampleInterval: Long,
        override val switchToThis: (String) -> Unit
) : CameraOperatorSource() {
    override val id: String = "UVC"
    override var selected = false
    private val connectionListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(usbDevice: UsbDevice) {
            Toast.makeText(context, "External Camera Attached", Toast.LENGTH_SHORT).show()
            usbMonitor.requestPermission(usbDevice)
        }

        override fun onConnect(usbDevice: UsbDevice, usbControlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            Toast.makeText(context, "External Camera Connected", Toast.LENGTH_SHORT).show()
            cameras.values.forEach { it.stopPreview() }
            val camera = UVCCamera()
            try {
                camera.open(usbControlBlock)
                cameras[usbDevice.deviceName] = UVCCameraOperator(
                        usbDevice.deviceName,
                        cameraView,
                        camera,
                        publisher
                )
                switchToThis(id)
                operatorSelector = {
                    it.first { it.id == usbDevice.deviceName }
                }
                if (!selectedCamera!!.isPreviewing() && selected) {
                    selectedCamera!!.startPreview()
                }
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }

        override fun onCancel(usbDevice: UsbDevice) {
        }

        override fun onDisconnect(usbDevice: UsbDevice, p1: USBMonitor.UsbControlBlock) {
            cameras[usbDevice.deviceName]?.release()
            cameras.remove(usbDevice.deviceName)
        }

        override fun onDettach(usbDevice: UsbDevice) {
            Toast.makeText(context, "External Camera Detached", Toast.LENGTH_SHORT).show()
        }

    }
    private val usbMonitor: USBMonitor = USBMonitor(context, connectionListener)
    private val publisher = PublishSubject.create<CameraPreview.PreviewFrame>()
    private val cameras = ConcurrentHashMap<String, UVCCameraOperator>()

    override fun onPreviewFrame(
            scheduler: Scheduler,
            processor: (CameraPreview.PreviewFrame) -> Unit
    ) = publisher.observeOn(scheduler).sample(sampleInterval, TimeUnit.MILLISECONDS).subscribe(processor)

    override fun onPreviewFrame(
            scheduler: Scheduler,
            frameConsumer: CameraPreview.FrameConsumer
    ) = publisher.observeOn(scheduler).sample(sampleInterval, TimeUnit.MILLISECONDS).subscribe { frameConsumer.accept(it) }

    override fun getCameras() = cameras.values.toList()

    override fun onSelected(operator: CameraPreviewOperator?) {
    }

    override fun start() {
        usbMonitor.register()
    }

    override fun close() {
        cameras.values.forEach { it.release() }
        usbMonitor.unregister()
    }

    class UVCCameraOperator(
            override val id: String,
            private val cameraView: CameraView,
            private val uvcCamera: UVCCamera,
            private val frameObserver: Observer<CameraPreview.PreviewFrame>
    ) : CameraPreviewOperator {
        private val supportedResolution = uvcCamera.supportedSizeList.map {
            Resolution(it.width, it.height)
        }.sortedByDescending { it.area }
        var resolutionSelector: (Iterable<Resolution>) -> Resolution? = HIGHEST_RESOLUTION
        private var selectedResolution = resolutionSelector(supportedResolution)
                ?: Resolution(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT)
        private val surface = cameraView.getPreview().let {
            when (it) {
                is Preview.Texture -> Surface(it.surfaceTexture)
                is Preview.Surface -> it.surfaceHolder.surface
            }
        }
        private val count = AtomicInteger(0)
        private var buffer = ByteArray(selectedResolution.area * 3 / 2)
        private val _isPreviewing = AtomicBoolean(false)

        override fun startPreview() {
            if (_isPreviewing.compareAndSet(false, true)) {
                selectedResolution.run {
                    cameraView.setPreviewResolution(selectedResolution)
                    uvcCamera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_YUYV)
                    buffer = ByteArray(area * 3 / 2)
                }
                uvcCamera.setPreviewDisplay(surface)
                uvcCamera.setFrameCallback {
                    runOnCompute {
                        synchronized(buffer) {
                            if (it.limit() > buffer.size) {
                                buffer = ByteArray(it.limit())
                            }
                            it.get(buffer)
                            frameObserver.onNext(CameraPreview.PreviewFrame("UVC-$id", selectedResolution, buffer, 0, count.getAndIncrement()))
                        }
                        if (count.get() > 1000) {
                            count.set(0)
                        }
                    }
                }
                uvcCamera.startPreview()
            }
        }

        override fun stopPreview() {
            if (_isPreviewing.compareAndSet(true, false)) {
                surface.release()
                uvcCamera.stopPreview()
                faceEngineTaskExecutor.cancelTasks(id)
            }
        }

        override fun isPreviewing() = _isPreviewing.get()

        fun release() = uvcCamera.run {
            try {
                setFrameCallback(null, UVCCamera.FRAME_FORMAT_YUYV)
                destroy()
            } catch (e: Throwable) {
                Log.w("Camera", "Error when release Camera", e)
            }
        }

        fun selectResolution(selector: (Iterable<Resolution>) -> Resolution?) {
            resolutionSelector = selector
        }

        fun selectResolution(selector: ResolutionSelector) {
            resolutionSelector = { selector.select(it) }
        }

        @FunctionalInterface
        interface ResolutionSelector {
            fun select(choices: Iterable<Resolution>): Resolution?
        }

        companion object {
            val HIGHEST_RESOLUTION: (Iterable<Resolution>) -> Resolution? = { it.firstOrNull() }
            val LOWEST_RESOLUTION: (Iterable<Resolution>) -> Resolution? = { it.lastOrNull() }
        }
    }
}

fun UVCCamera.setFrameCallback(format: Int = UVCCamera.PIXEL_FORMAT_YUV420SP, callback: (ByteBuffer) -> Unit) = setFrameCallback(callback, format)