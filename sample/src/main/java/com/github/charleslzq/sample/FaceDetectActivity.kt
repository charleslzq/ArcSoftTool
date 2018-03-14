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
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceEngineService
import com.github.charleslzq.arcsofttools.kotlin.DefaultArcSoftEngineService
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.facestore.ReadWriteFaceStore
import kotlinx.android.synthetic.main.activity_face_detect.*

class FaceDetectActivity : AppCompatActivity() {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            faceEngineService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            @Suppress("UNCHECKED_CAST")
            faceEngineService =
                    service as ArcSoftFaceEngineService<ReadWriteFaceStore<Person, Face>>
        }

    }
    private var faceEngineService: ArcSoftFaceEngineService<ReadWriteFaceStore<Person, Face>>? =
            null
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.onPreviewFrame {
            Log.i(TAG, "on frame with size ${it.size} and rotation ${it.rotation}")
            val detectResult = faceEngineService?.detect(it) ?: emptyList()
            val detectedAge = faceEngineService?.detectAge(it) ?: emptyList()
            if (detectResult.size != 1 || detectedAge.size != 1) {
                val result = detectResult.mapNotNull { faceEngineService!!.search(it) }
                if (result.isNotEmpty()) {
                    val person = result.maxBy { it.second } ?: Pair(Person("", ""), 0f)
                    if (person.second > 0) {
                        toast(buildString {
                            append("Match Result ${person.first.name}")
                            if (detectedAge[0].age > 0) {
                                append(" with detected age ${detectedAge[0].age}")
                            } else {
                                append(", fail to detect age")
                            }
                            append(", ${++count}")
                        })
                        Log.i("test", "match result : $person")
                        setResult(Activity.RESULT_OK, Intent().apply {
                            putExtra("personName", person.first.name)
                        })
                        finish()
                    }
                }
                toast("No Match Result, ${++count}")
            } else {
                toast("No or too much (${detectResult.size}) Face(s) Detected, ${++count}")
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
