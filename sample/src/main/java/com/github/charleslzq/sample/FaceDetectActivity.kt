package com.github.charleslzq.sample

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceEngineService
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftEngineService
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore
import kotlinx.android.synthetic.main.activity_face_detect.*

class FaceDetectActivity : AppCompatActivity() {
    private val fileBase = Environment.getExternalStorageDirectory().absolutePath + "/tmp_pics/"

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            faceEngineService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            @Suppress("UNCHECKED_CAST")
            faceEngineService =
                    service as ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>>
            faceEngineService!!.store.refresh()
        }

    }
    private var faceEngineService: ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>>? =
            null
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.setOnClickListener {
            val originalId = faceDetectCamera.selectedCamera?.id
            faceDetectCamera.getCurrentSource()?.operatorSelector = { it.firstOrNull { it.id != originalId } }
        }
        faceDetectCamera.onPreviewFrame {
            try {
                Log.i(TAG, "on frame with size ${it.size} and rotation ${it.rotation}, ${it.sequence}")
                if (it.sequence == 7) {
                    Log.i(TAG, "limit reached")
                }
                val trackFaces = emptyList<TrackedFace>()
                if (trackFaces.size == 1) {
                    val detectResult = faceEngineService?.detect(it) ?: emptyMap()
                    faceDetectCamera.updateTrackFaces(detectResult.keys)
                    val detectedAge = faceEngineService?.detectAge(it)?.takeIf { it.size == 1 }?.get(0)?.age
                    val detectedGender = faceEngineService?.detectGender(it)?.takeIf { it.size == 1 }?.get(0)?.gender
                    var personName: String? = null
                    toast(buildString {
                        if (detectResult.size == 1) {
                            val result = detectResult.mapNotNull { faceEngineService!!.search(it.value) }
                            if (result.isNotEmpty()) {
                                val person = result.maxBy { it.second } ?: Pair(Person("", ""), 0f)
                                if (person.second > 0.5f) {
                                    personName = person.first.name
                                    append("Match Result $personName score ${person.second}")
                                } else {
                                    append("No Match Face")
                                }
                            } else {
                                append("No Match Result")
                            }
                        } else {
                            append("No or too much (${detectResult.size}) Face(s) Detected")
                        }
                        append(", ")
                        if (detectedAge != null) {
                            append("detected age $detectedAge")
                        } else {
                            append("fail to detect age")
                        }
                        append(", ")
                        if (detectedGender != null) {
                            append("gender $detectedGender")
                        } else {
                            append("fail to detect gender")
                        }
                        append(", ")
                        append("${++count}")
                    }.also {
                        Log.i(TAG, it)
                    })
                    personName?.let {
                        setResult(Activity.RESULT_OK, Intent().apply {
                            putExtra("personName", it)
                        })
//                    finish()
                    }
                } else {
                    toast("0 face(s) detected! ${it.sequence}")
                }
            } catch (throwable: Throwable) {
                Log.i(TAG, "Exception occur", throwable)
            }
        }
        bindService(
                Intent(this, WebSocketArcSoftEngineService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        faceDetectCamera.start()
    }

    override fun onPause() {
        faceDetectCamera.pause()
        super.onPause()
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        faceDetectCamera.close()
        super.onDestroy()
    }

    companion object {
        const val TAG = "FaceDetectActivity"
    }
}
