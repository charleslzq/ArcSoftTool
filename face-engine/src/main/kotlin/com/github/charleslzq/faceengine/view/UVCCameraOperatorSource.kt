package com.github.charleslzq.faceengine.view

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import android.view.Surface
import com.github.charleslzq.faceengine.view.config.*
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.view.CameraView
import io.fotoapparat.view.Preview
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class UVCCameraOperatorSource(
        context: Context,
        cameraView: CameraView,
        val submit: (ByteBuffer, (ByteBuffer) -> SourceAwarePreviewFrame) -> Unit,
        val onNewDevice: (CameraPreviewOperator) -> Unit,
        val onDisconnect: (CameraPreviewOperator) -> Unit
) : CameraOperatorSource() {
    override val id: String = "UVC"
    private val connectionListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(usbDevice: UsbDevice) {
            usbMonitor.requestPermission(usbDevice)
        }

        override fun onConnect(usbDevice: UsbDevice, usbControlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            val camera = UVCCamera()
            try {
                camera.open(usbControlBlock)
                val newCamera = UVCCameraOperator(
                        usbDevice.deviceName,
                        this@UVCCameraOperatorSource,
                        cameraView,
                        camera,
                        submit
                )
                cameraMap[usbDevice.deviceName] = newCamera
                onNewDevice(newCamera)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }

        override fun onCancel(usbDevice: UsbDevice) {
        }

        override fun onDisconnect(usbDevice: UsbDevice, p1: USBMonitor.UsbControlBlock) {
            cameraMap[usbDevice.deviceName]?.run {
                onDisconnect(this)
                release()
            }
            cameraMap.remove(usbDevice.deviceName)
        }

        override fun onDettach(usbDevice: UsbDevice) {
        }
    }
    private val usbMonitor: USBMonitor = USBMonitor(context, connectionListener)
    private val cameraMap = ConcurrentHashMap<String, UVCCameraOperator>()
    override val cameras: List<CameraPreviewOperator>
        get() = cameraMap.values.toList()

    override fun open() {
        usbMonitor.register()
    }

    override fun close() {
        cameraMap.values.forEach { it.release() }
        usbMonitor.unregister()
    }

    class UVCCameraOperator(
            override val id: String,
            override val source: CameraOperatorSource,
            private val cameraView: CameraView,
            private val uvcCamera: UVCCamera,
            val submit: (ByteBuffer, (ByteBuffer) -> SourceAwarePreviewFrame) -> Unit
    ) : CameraPreviewOperator {
        private val surface
            get() = cameraView.getPreview().let {
                when (it) {
                    is Preview.Texture -> Surface(it.surfaceTexture)
                    is Preview.Surface -> it.surfaceHolder.surface
                }
            }
        private val count = AtomicInteger(0)
        private var buffer = ByteArray(0)
        private val _isPreviewing = AtomicBoolean(false)

        override fun startPreview(request: CameraPreviewRequest) {
            if (_isPreviewing.compareAndSet(false, true)) {
                val capabilities = getCapabilities()
                val currentParameters = getCurrentParameters()
                capabilities.previewResolutions.let {
                    request.resolutionSelector.instance(it)
                }?.run {
                    cameraView.setPreviewResolution(this)
                    uvcCamera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_YUYV)
                    buffer = ByteArray(area * 3 / 2)
                }
                uvcCamera.setPreviewDisplay(surface)
                uvcCamera.setFrameCallback {
                    submit(it) {
                        synchronized(buffer) {
                            if (it.limit() > buffer.size) {
                                buffer = ByteArray(it.limit())
                            }
                            it.get(buffer)
                            SourceAwarePreviewFrame("UVC-$id", count.getAndIncrement(), currentParameters.resolution, buffer, 0)
                        }
                    }
                    if (count.get() > 1000) {
                        count.set(0)
                    }
                }
                uvcCamera.startPreview()
            }
        }

        override fun stopPreview() {
            if (_isPreviewing.compareAndSet(true, false)) {
                surface.release()
                uvcCamera.stopPreview()
            }
        }

        override fun isPreviewing() = _isPreviewing.get()

        override fun getCapabilities(): CameraCapabilities = UVCCameraCapabilities(uvcCamera.supportedSizeList.map {
            Resolution(it.width, it.height)
        })

        override fun getCurrentParameters(): CameraParameters = UVCCameraParameters(uvcCamera.previewSize.let { Resolution(it.width, it.height) })

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