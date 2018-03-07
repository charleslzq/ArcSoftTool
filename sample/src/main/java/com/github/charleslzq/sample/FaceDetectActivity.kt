package com.github.charleslzq.sample

import android.app.Activity
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
import com.github.charleslzq.faceengine.core.kotlin.FaceEngineService
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.onNewPicture {
            counter.text = it.first.toString()
            resultDisplay.setImageBitmap(it.second)
            val result =
                faceEngineService?.detect(it.second)?.mapNotNull { faceEngineService!!.search(it) }
                        ?: emptyList()
            if (result.isNotEmpty()) {
                val person = result.maxBy { it.second }!!.first
                Log.i("test", "match result : $person")
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("personName", person.name)
                })
                finish()
            } else {
                Log.i("test", "match result not found ${it.first}")
                if (it.first >= CHECK_LIMIT) {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                } else if (it.first >= CHECK_LIMIT / 2) {
                    faceDetectCamera.period(faceDetectCamera.interval.toLong() * 2) {
                        it.takePicture()
                    }
                }
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

    companion object {
        private const val CHECK_LIMIT = 8
    }
}
