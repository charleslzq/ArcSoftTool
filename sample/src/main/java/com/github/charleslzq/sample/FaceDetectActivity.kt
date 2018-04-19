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
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceEngineService
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftEngineService
import com.github.charleslzq.faceengine.support.faceEngineTaskExecutor
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore
import com.orhanobut.logger.Logger
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.setOnClickListener {
            faceDetectCamera.selectNext()
        }
        faceDetectCamera.onPreview {
            val startTime = System.currentTimeMillis()
            try {
                Logger.i("on frame with size ${it.size} and rotation ${it.rotation}, ${it.sequence}/${it.source}")
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
                    append("${it.sequence}/${it.source}")
                }.also {
                    Logger.i(it)
                })
                personName?.let {
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra("personName", it)
                    })
                    finish()
                }
            } catch (throwable: Throwable) {
                Logger.e(throwable, "Exception occur with frame ${it.sequence}")
            }
            val endTime = System.currentTimeMillis()
            Logger.i("Handle completed for frame ${it.sequence}, use time ${endTime - startTime}")
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
        faceEngineTaskExecutor.cancelTasks()
        super.onDestroy()
    }
}
