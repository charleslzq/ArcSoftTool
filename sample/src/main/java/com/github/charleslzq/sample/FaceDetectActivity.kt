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
import com.github.charleslzq.arcsofttools.kotlin.DefaultArcSoftEngineService
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.faceengine.core.kotlin.FaceEngineService
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore
import com.github.charleslzq.faceengine.core.kotlin.support.convert
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
        faceDetectCamera.onPreviewFrame {
            val image = convert(it)
            Log.i(TAG, "on frame with size ${it.size} and rotation ${it.rotation}")
            val detectResult = faceEngineService?.detect(image) ?: emptyList()
            if (detectResult.isNotEmpty()) {
                val result = detectResult.mapNotNull { faceEngineService!!.search(it) }
                if (result.isNotEmpty()) {
                    val person = result.maxBy { it.second } ?: Pair(Person("", ""), 0f)
                    if (person.second > 0) {
                        toast("Match Result ${person.first.name}, ${++count}")
                        Log.i("test", "match result : $person")
                        setResult(Activity.RESULT_OK, Intent().apply {
                            putExtra("personName", person.first.name)
                        })
                        finish()
                    }
                }
                toast("No Match Result, ${++count}")
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

    companion object {
        const val TAG = "FaceDetectActivity"
    }
}
