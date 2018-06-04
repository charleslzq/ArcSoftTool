package com.github.charleslzq.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftService
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_face_detect.*

class FaceDetectActivity : AppCompatActivity() {
    private val connection = WebSocketArcSoftService.getBuilder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.onPreview {
            Logger.i("on frame with size ${it.size} and rotation ${it.rotation}, ${it.sequence}/${it.source}")
            val startTime = System.currentTimeMillis()
            connection.whenConnected { engine ->
                try {
                    val detectResult = engine.detect(it)
                    faceDetectCamera.updateTrackFaces(detectResult.keys)
                    val detectedAge = engine.detectAge(it).takeIf { it.size == 1 }?.get(0)?.age
                    val detectedGender = engine.detectGender(it).takeIf { it.size == 1 }?.get(0)?.gender
                    var personName: String? = null
                    toast(buildString {
                        if (detectResult.size == 1) {
                            val result = detectResult.mapNotNull { engine.searchForScore(it.value) }
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
            }
            val endTime = System.currentTimeMillis()
            Logger.i("Handle completed for frame ${it.sequence}, use time ${endTime - startTime}")
        }
        connection.bind<WebSocketArcSoftService>(this)
    }

    override fun onResume() {
        super.onResume()
        faceDetectCamera.open()
    }

    override fun onPause() {
        faceDetectCamera.pause()
        super.onPause()
    }

    override fun onDestroy() {
        unbindService(connection)
        faceDetectCamera.close()
        super.onDestroy()
    }
}
