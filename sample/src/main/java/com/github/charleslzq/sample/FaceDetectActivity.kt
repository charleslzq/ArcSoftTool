package com.github.charleslzq.sample

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.charleslzq.arcsofttools.kotlin.DefaultArcSoftEngineService
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.faceengine.core.kotlin.FaceEngineService
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore
import com.guo.android_extend.java.ExtByteArrayOutputStream
import io.fotoapparat.preview.Frame
import kotlinx.android.synthetic.main.activity_face_detect.*

class FaceDetectActivity : AppCompatActivity() {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            faceEngineService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            @Suppress("UNCHECKED_CAST")
            faceEngineService =
                    service as FaceEngineService<Person, Face, Float, ReadWriteFaceStore<Person, Face>>
        }

    }
    private var faceEngineService: FaceEngineService<Person, Face, Float, ReadWriteFaceStore<Person, Face>>? =
        null
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.onNewFrame {
            val image = convert(it)
            Log.i(TAG, "on frame with size ${it.size} and rotation ${it.rotation}")
            val detectResult = faceEngineService?.detect(image)
            if (detectResult != null) {
                val result = detectResult.mapNotNull { faceEngineService!!.search(it) }
                if (result.isNotEmpty()) {
                    val person = result.maxBy { it.second }!!.first
                    toast("Match Result ${person.name}, ${++count}")
                    Log.i("test", "match result : $person")
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra("personName", person.name)
                    })
                    finish()
                } else {
                    toast("No Match Result, ${++count}")
                }
            } else {
                toast("No Face Detected, ${++count}")
            }
        }
        bindService(
            Intent(this, DefaultArcSoftEngineService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        faceDetectCamera.start()
    }

    override fun onPause() {
        faceDetectCamera.stop()
        super.onPause()
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        faceDetectCamera.stop()
        super.onDestroy()
    }

    private fun convert(frame: Frame) =
        YuvImage(frame.image, ImageFormat.NV21, frame.size.width, frame.size.height, null).run {
            ExtByteArrayOutputStream().use {
                compressToJpeg(Rect(0, 0, frame.size.width, frame.size.height), 100, it)
                BitmapFactory.decodeByteArray(it.byteArray, 0, it.byteArray.size)
            }
        }

    companion object {
        const val TAG = "FaceDetectActivity"
    }
}
