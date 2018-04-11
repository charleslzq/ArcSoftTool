package com.github.charleslzq.faceengine.view

import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbDevice
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.github.charleslzq.faceengine.core.R
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.runOnCompute
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usbcameracommon.UVCCameraHandler
import com.serenegiant.widget.UVCCameraTextureView
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean

internal class UVCCameraAdapter
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0) :
        FrameLayout(context, attributeSet, defStyle), CameraInterface {
    private val uvcCameraTextureView = UVCCameraTextureView(context, attributeSet, defStyle).also {
        addView(it)
    }
    private val trackView = TrackView(context, attributeSet, defStyle).also { addView(it) }
    private val _isRunning = AtomicBoolean(false)
    private val connectionListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onConnect(usbDevice: UsbDevice, usbControlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            Toast.makeText(context, "Camera Connected", Toast.LENGTH_SHORT).show()
            initHandler()
            uvcCameraHandler.startPreview(Surface(uvcCameraTextureView.surfaceTexture))
        }

        override fun onCancel(usbDevice: UsbDevice) {
            Toast.makeText(context, "Camera Canceled", Toast.LENGTH_SHORT).show()
        }

        override fun onAttach(usbDevice: UsbDevice) {
            Toast.makeText(context, "Camera Attached", Toast.LENGTH_LONG).show()
            usbMonitor.requestPermission(usbDevice)
        }

        override fun onDisconnect(usbDevice: UsbDevice, p1: USBMonitor.UsbControlBlock) {
            Toast.makeText(context, "Camera Disconnected", Toast.LENGTH_SHORT).show()
            uvcCameraHandler.stopPreview()
        }

        override fun onDettach(usbDevice: UsbDevice) {
            Toast.makeText(context, "Camera Detached", Toast.LENGTH_SHORT).show()
        }

    }
    private val usbMonitor: USBMonitor = USBMonitor(context, connectionListener)
    private lateinit var activity: Activity
    private lateinit var uvcCameraHandler: UVCCameraHandler

    override fun start(activity: Activity) {
        if (_isRunning.compareAndSet(false, true)) {
            this.activity = activity
            usbMonitor.register()
            initHandler()
            visibility = View.VISIBLE
            uvcCameraTextureView.onResume()
            uvcCameraHandler.startPreview(Surface(uvcCameraTextureView.surfaceTexture))
        }
    }

    override fun pause() {
        if (_isRunning.compareAndSet(true, false)) {
            usbMonitor.unregister()
            if (uvcCameraHandler.isPreviewing) {
                uvcCameraHandler.stopPreview()
            }
            uvcCameraTextureView.onPause()
            visibility = View.INVISIBLE
        }
    }

    override fun stop() {
        if (_isRunning.compareAndSet(true, false)) {
            usbMonitor.destroy()
            if (uvcCameraHandler.isPreviewing) {
                uvcCameraHandler.stopPreview()
            }
            uvcCameraTextureView.onPause()
            visibility = View.INVISIBLE
        }
    }

    override fun isRunning() = _isRunning.get()

    override fun updateTrackFaces(faces: List<TrackedFace>) {
        if (trackView.track) {
            trackView.resetRects(faces)
        }
    }

    override fun hasAvailableCamera() = usbMonitor.getDeviceList(DeviceFilter.getDeviceFilters(context, R.xml.device_filter)[0]).size > 0

    override fun onPreviewFrame(scheduler: Scheduler, processor: (CameraPreview.PreviewFrame) -> Unit): Disposable {
        return runOnCompute { }
    }

    override fun onPreviewFrame(scheduler: Scheduler, frameConsumer: CameraPreview.FrameConsumer): Disposable {
        return runOnCompute { }
    }

    private fun initHandler() {
        if (::activity.isInitialized) {
            if (!::uvcCameraHandler.isInitialized) {
                uvcCameraHandler = UVCCameraHandler.createHandler(activity, uvcCameraTextureView, 1, width, height, UVCCamera.FRAME_FORMAT_YUYV)
            }
        } else {
            throw IllegalStateException("Activity Not Initialized")
        }
    }
}