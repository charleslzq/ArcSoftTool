package com.github.charleslzq.faceengine.view

import android.content.Context
import android.hardware.usb.UsbDevice
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.github.charleslzq.faceengine.core.R
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.runOnCompute
import com.github.charleslzq.faceengine.support.runOnUI
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import io.fotoapparat.parameter.Resolution
import io.reactivex.Scheduler
import io.reactivex.subjects.PublishSubject
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

internal class UVCCameraAdapter
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0) :
        FrameLayout(context, attributeSet, defStyle), CameraInterface {
    private val uvcCameraTextureView = UVCCameraTextureView(context, attributeSet, defStyle).also {
        addView(it)
    }
    private var surface: Surface? = null
    private val trackView = TrackView(context, attributeSet, defStyle).also { addView(it) }

    private val _isRunning = AtomicBoolean(false)

    private val connectionListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onConnect(usbDevice: UsbDevice, usbControlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            Log.i("Camera", "connect Camera")
            Toast.makeText(context, "Camera Connected", Toast.LENGTH_SHORT).show()
            releaseCamera()
            runOnCompute {
                val camera = UVCCamera()
                camera.open(usbControlBlock)
                supportedResolution.clear()
                supportedResolution.addAll(camera.supportedSizeList.map {
                    Resolution(it.width, it.height)
                }.sortedByDescending { it.area })
                uvcCameraTextureView.surfaceTexture?.let {
                    surface = Surface(it)
                    startPreview(camera, surface!!)

                    synchronized(cameraLock) {
                        uvcCamera = camera
                    }
                }
            }
        }

        override fun onCancel(usbDevice: UsbDevice) {
            Log.i("Camera", "cancel Camera")
            Toast.makeText(context, "Camera Canceled", Toast.LENGTH_SHORT).show()
        }

        override fun onAttach(usbDevice: UsbDevice) {
            Log.i("Camera", "attach Camera")
            Toast.makeText(context, "Camera Attached", Toast.LENGTH_LONG).show()
            usbMonitor.requestPermission(usbDevice)
        }

        override fun onDisconnect(usbDevice: UsbDevice, p1: USBMonitor.UsbControlBlock) {
            Log.i("Camera", "disconnect Camera")
            Toast.makeText(context, "Camera Disconnected", Toast.LENGTH_SHORT).show()
            releaseCamera()
        }

        override fun onDettach(usbDevice: UsbDevice) {
            Log.i("Camera", "detach Camera")
            Toast.makeText(context, "Camera Detached", Toast.LENGTH_SHORT).show()
        }

    }
    private val usbMonitor: USBMonitor = USBMonitor(context, connectionListener)
    private val cameraLock = Object()
    private var uvcCamera: UVCCamera? = null
    private val supportedResolution = mutableListOf<Resolution>()
    private var selectedResolution = Resolution(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT)
    private val publisher = PublishSubject.create<CameraPreview.PreviewFrame>()

    override fun onResume() {
        if (_isRunning.compareAndSet(false, true)) {
            synchronized(cameraLock) {
                usbMonitor.register()
                uvcCamera?.startPreview()
                visibility = View.VISIBLE
            }
        }
    }

    override fun onPause() {
        if (_isRunning.compareAndSet(true, false)) {
            synchronized(cameraLock) {
                usbMonitor.unregister()
                uvcCamera?.stopPreview()
            }
        }
    }

    override fun onDestroy() {
        if (_isRunning.compareAndSet(true, false)) {
            synchronized(cameraLock) {
                releaseCamera()
                usbMonitor.destroy()
                uvcCamera = null
                visibility = View.INVISIBLE
            }
        }
    }

    override fun isRunning() = _isRunning.get()

    override fun updateTrackFaces(faces: List<TrackedFace>) {
        if (trackView.track) {
            trackView.resetRects(faces)
        }
    }

    override fun hasAvailableCamera() = usbMonitor.getDeviceList(DeviceFilter.getDeviceFilters(context, R.xml.device_filter)[0]).size > 0

    override fun onPreviewFrame(
            scheduler: Scheduler,
            processor: (CameraPreview.PreviewFrame) -> Unit
    ) = publisher.observeOn(scheduler).subscribe(processor)

    override fun onPreviewFrame(
            scheduler: Scheduler,
            frameConsumer: CameraPreview.FrameConsumer
    ) = publisher.observeOn(scheduler).subscribe { frameConsumer.accept(it) }

    private fun startPreview(camera: UVCCamera, surface: Surface) {
        Log.i("Camera", "Start Camera Preview")
        if (supportedResolution.isNotEmpty()) {
            selectedResolution = supportedResolution[0]
            selectedResolution.run {
                runOnUI {
                    uvcCameraTextureView.setAspectRatio(width, height)
                }
                camera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_YUYV)
            }
        }
        camera.setPreviewDisplay(surface)
        camera.setFrameCallback {
            runOnCompute {
                Log.i("Camera", "Received ${it.remaining()} data")
                val bytes = ByteArray(selectedResolution.area * 3 / 2)
                it.deepCopy().get(bytes)
                publisher.onNext(CameraPreview.PreviewFrame(selectedResolution, bytes, 0))
            }
        }
        camera.startPreview()
    }

    private fun releaseCamera() {
        synchronized(cameraLock) {
            uvcCamera?.run {
                try {
                    setFrameCallback(null, UVCCamera.FRAME_FORMAT_YUYV)
                    close()
                    destroy()
                } catch (e: Throwable) {
                    Log.w("Camera", "Error when release Camera", e)
                }
                uvcCamera = null
            }
            surface?.run {
                release()
                surface = null
            }
        }
    }
}

private fun UVCCamera.setFrameCallback(format: Int = UVCCamera.PIXEL_FORMAT_NV21, callback: (ByteBuffer) -> Unit) = setFrameCallback(callback, format)

fun ByteBuffer.deepCopy(): ByteBuffer {
    val newBuffer = if (isDirect) {
        ByteBuffer.allocateDirect(remaining())
    } else {
        ByteBuffer.allocate(remaining())
    }
    val readOnlyBuffer = asReadOnlyBuffer()
    readOnlyBuffer.flip()
    newBuffer.put(readOnlyBuffer)
    return newBuffer
}