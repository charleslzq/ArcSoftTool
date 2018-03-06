package com.github.charleslzq.sample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftEngineService
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.faceengine.core.kotlin.FaceEngineServiceImpl
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_face_detect.*

class FaceDetectActivity : AppCompatActivity() {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            faceEngineService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            @Suppress("UNCHECKED_CAST")
            faceEngineService = service as FaceEngineServiceImpl<Person, Face, Float>
        }

    }
    private var faceEngineService: FaceEngineServiceImpl<Person, Face, Float>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.autoTakePictureCallback.publisher.observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                counter.text = faceDetectCamera.count.toString()
                resultDisplay.setImageBitmap(it)
                val result =
                    faceEngineService?.detect(it)?.mapNotNull { faceEngineService!!.search(it) }
                            ?: emptyList()
                if (result.isNotEmpty()) {
                    val person = result.maxBy { it.second }!!.first
                    Log.i("test", "match result : $person")
                } else {
                    Log.i("test", "match result not found")
                }
            }
        bindService(
            Intent(this, ArcSoftEngineService::class.java),
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
        super.onDestroy()
    }
}
