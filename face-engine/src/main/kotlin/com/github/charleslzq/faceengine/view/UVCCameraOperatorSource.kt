package com.github.charleslzq.faceengine.view

import android.content.Context
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
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class UVCCameraOperatorSource(
        context: Context,
        cameraView: CameraView,
        consumeFrame: (SourceAwarePreviewFrame) -> Unit,
        override var cameraPreviewConfiguration: CameraPreviewConfiguration,
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
            cameraMap.values.forEach { it.stopPreview() }
            val camera = UVCCamera()
            try {
                camera.open(usbControlBlock)
                cameraMap[usbDevice.deviceName] = UVCCameraOperator(
                        usbDevice.deviceName,
                        cameraView,
                        camera,
                        consumeFrame,
                        cameraPreviewConfiguration
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
            cameraMap[usbDevice.deviceName]?.release()
            cameraMap.remove(usbDevice.deviceName)
        }

        override fun onDettach(usbDevice: UsbDevice) {
            Toast.makeText(context, "External Camera Detached", Toast.LENGTH_SHORT).show()
        }
    }
    private val usbMonitor: USBMonitor = USBMonitor(context, connectionListener)
    private val cameraMap = ConcurrentHashMap<String, UVCCameraOperator>()
    override val cameras: List<CameraPreviewOperator>
        get() = cameraMap.values.toList()

    override fun onSelected(operator: CameraPreviewOperator?) {
    }

    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {
        super.applyConfiguration(cameraPreviewConfiguration)
        cameraMap.values.forEach { it.applyConfiguration(cameraPreviewConfiguration) }
    }

    override fun start() {
        usbMonitor.register()
    }

    override fun close() {
        cameraMap.values.forEach { it.release() }
        usbMonitor.unregister()
    }

    class UVCCameraOperator(
            override val id: String,
            private val cameraView: CameraView,
            private val uvcCamera: UVCCamera,
            private val consumeFrame: (SourceAwarePreviewFrame) -> Unit,
            private var cameraPreviewConfiguration: CameraPreviewConfiguration
    ) : CameraPreviewOperator {
        private val supportedResolution = uvcCamera.supportedSizeList.map {
            Resolution(it.width, it.height)
        }.sortedByDescending { it.area }
        private var selectedResolution = cameraPreviewConfiguration.previewResolution(supportedResolution)
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
                            consumeFrame(SourceAwarePreviewFrame("UVC-$id", count.getAndIncrement(), selectedResolution, buffer, 0))
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

        override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {
            this.cameraPreviewConfiguration = cameraPreviewConfiguration
        }
    }
}

fun UVCCamera.setFrameCallback(format: Int = UVCCamera.PIXEL_FORMAT_YUV420SP, callback: (ByteBuffer) -> Unit) = setFrameCallback(callback, format)