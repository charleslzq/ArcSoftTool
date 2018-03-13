package com.github.charleslzq.sample

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.github.charleslzq.arcsofttools.kotlin.DefaultArcSoftEngineService
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.faceengine.core.FaceEngineService
import com.github.charleslzq.facestore.ReadWriteFaceStore
import kotlinx.android.synthetic.main.activity_main.*

fun Context.toast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(this, message, duration).show()

class MainActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        captureImageButton.setOnClickListener {
            startActivityForResult(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    RequestCodes.IMAGE_CAMERA.code
            )
        }
        checkFaceButton.setOnClickListener {
            startActivityForResult(
                    Intent(this, FaceDetectActivity::class.java),
                    RequestCodes.FACE_CHECK.code
            )
        }
        bindService(
                Intent(this, DefaultArcSoftEngineService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (RequestCodes.fromCode(requestCode)) {
            RequestCodes.IMAGE_CAMERA -> if (resultCode == Activity.RESULT_OK && data != null) {
                faceEngineService?.detect(data.extras["data"] as Bitmap)?.run {
                    if (isNotEmpty()) {
                        val testPersonId = "test"
                        faceEngineService!!.store.savePerson(Person(testPersonId, "test_name"))
                        forEach {
                            faceEngineService!!.store.saveFace(testPersonId, it)
                        }
                    }
                }
            }
            RequestCodes.FACE_CHECK -> if (resultCode == Activity.RESULT_OK && data != null) {
                val personName = data.extras["personName"] as String
                toast("Found Person $personName")
            } else if (resultCode == Activity.RESULT_CANCELED) {
                toast("Fail to identify face")
            }
        }
    }

    enum class RequestCodes {
        IMAGE_CAMERA,
        FACE_CHECK;

        val code
            get() = ordinal + 1

        companion object {
            @JvmStatic
            fun fromCode(requestCode: Int) = RequestCodes.values()[requestCode - 1]
        }
    }
}
