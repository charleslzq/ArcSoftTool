package com.github.charleslzq.faceengine.view

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import android.widget.Toast
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import io.fotoapparat.view.CameraView

class UVCCameraSource(context: Context, cameraView: CameraView) {
    private val connectionListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onConnect(usbDevice: UsbDevice, usbControlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            Log.i("Camera", "connect Camera")
            Toast.makeText(context, "Camera Connected", Toast.LENGTH_SHORT).show()
//            releaseCamera()
//            runOnCompute {
//                val camera = UVCCamera()
//                camera.open(usbControlBlock)
//                supportedResolution.clear()
//                supportedResolution.addAll(camera.supportedSizeList.map {
//                    Resolution(it.width, it.height)
//                }.sortedByDescending { it.area })
//                surface = cameraView.getPreview().let {
//                    when (it) {
//                        is Preview.Texture -> Surface(it.surfaceTexture)
//                        is Preview.Surface -> it.surfaceHolder.surface
//                    }
//                }
//                startPreview(camera, surface!!)
//                synchronized(cameraLock) {
//                    uvcCamera = camera
//                }
//            }
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
//            releaseCamera()
        }

        override fun onDettach(usbDevice: UsbDevice) {
            Log.i("Camera", "detach Camera")
            Toast.makeText(context, "Camera Detached", Toast.LENGTH_SHORT).show()
        }

    }
    private val usbMonitor: USBMonitor = USBMonitor(context, connectionListener)
    private val cameraLock = Object()
    private var uvcCamera: UVCCamera? = null

    fun getCameras(): List<CameraPreviewOperator> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}