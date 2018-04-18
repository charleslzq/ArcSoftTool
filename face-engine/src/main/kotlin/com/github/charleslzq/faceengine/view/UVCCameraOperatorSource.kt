package com.github.charleslzq.faceengine.view

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.util.Log
import android.view.Surface
import android.widget.Toast
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
        override val sampleInterval: Long
) : CameraOperatorSource() {
    override var selected = false
    private val connectionListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(usbDevice: UsbDevice) {
            Toast.makeText(context, "Camera Attached", Toast.LENGTH_SHORT).show()
            usbMonitor.requestPermission(usbDevice)
        }

        override fun onConnect(usbDevice: UsbDevice, usbControlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            Toast.makeText(context, "Camera Connected", Toast.LENGTH_SHORT).show()
            cameras.values.forEach { it.stopPreview() }
            runOnCompute {
                val camera = UVCCamera()
                try {
                    camera.open(usbControlBlock)
                    cameras[usbDevice.deviceName] = UVCCameraOperator(
                            usbDevice.deviceName,
                            cameraView,
                            camera,
                            publisher
                    )
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
        }

        override fun onCancel(usbDevice: UsbDevice) {
        }

        override fun onDisconnect(usbDevice: UsbDevice, p1: USBMonitor.UsbControlBlock) {
            Toast.makeText(context, "Camera Disconnected", Toast.LENGTH_SHORT).show()
            cameras[usbDevice.deviceName]?.release()
            cameras.remove(usbDevice.deviceName)
        }

        override fun onDettach(usbDevice: UsbDevice) {
            Toast.makeText(context, "Camera Detached", Toast.LENGTH_SHORT).show()
        }

    }
    private val usbMonitor: USBMonitor = USBMonitor(context, connectionListener)
    private val publisher = PublishSubject.create<CameraPreview.PreviewFrame>()
    private val cameras = ConcurrentHashMap<String, UVCCameraOperator>()

    override fun onPreviewFrame(
            scheduler: Scheduler,
            processor: (CameraPreview.PreviewFrame) -> Unit
    ) = publisher.sample(sampleInterval, TimeUnit.MILLISECONDS).observeOn(scheduler).subscribe(processor)

    override fun onPreviewFrame(
            scheduler: Scheduler,
            frameConsumer: CameraPreview.FrameConsumer
    ) = publisher.sample(sampleInterval, TimeUnit.MILLISECONDS).observeOn(scheduler).subscribe { frameConsumer.accept(it) }

    override fun getCameras() = cameras.values.toList()

    override fun onSelected(operator: CameraPreviewOperator?) {
    }

    override fun start() {
        usbMonitor.register()
    }

    override fun close() {
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
        private var selectedResolution = if (supportedResolution.isNotEmpty()) {
            supportedResolution[0]
        } else {
            Resolution(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT)
        }
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
                            frameObserver.onNext(CameraPreview.PreviewFrame(selectedResolution, buffer, 0, count.getAndIncrement()))
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
                uvcCamera.stopPreview()
            }
        }

        override fun isPreviewing() = _isPreviewing.get()

        override fun takePicture(): Bitmap {
            throw UnsupportedOperationException("Image Capture not supported currently")
        }

        fun release() = uvcCamera.run {
            try {
                setFrameCallback(null, UVCCamera.FRAME_FORMAT_YUYV)
                destroy()
            } catch (e: Throwable) {
                Log.w("Camera", "Error when release Camera", e)
            }
        }

    }
}

fun UVCCamera.setFrameCallback(format: Int = UVCCamera.PIXEL_FORMAT_YUV420SP, callback: (ByteBuffer) -> Unit) = setFrameCallback(callback, format)