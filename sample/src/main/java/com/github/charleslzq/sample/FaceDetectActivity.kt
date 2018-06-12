package com.github.charleslzq.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftService
import com.github.charleslzq.faceengine.support.runOnIo
import com.github.charleslzq.faceengine.view.FotoCameraOperatorSource
import com.github.charleslzq.faceengine.view.config.FotoCameraPreviewRequest
import com.github.charleslzq.faceengine.view.config.ResolutionSelector
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_face_detect.*
import java.util.concurrent.CountDownLatch

class FaceDetectActivity : AppCompatActivity() {
    private val connection = WebSocketArcSoftService.getBuilder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.setOnClickListener {
            faceDetectCamera.selectNext()
        }
        val start = System.currentTimeMillis()
        Log.i("Capabilities", "Start at $start")
        runOnIo {
            faceDetectCamera.cameras.forEach {
                val needStart = it is FotoCameraOperatorSource.FotoCameraPreviewOperator
                val latch = CountDownLatch(1)
                if (needStart) {
                    it.startPreview(FotoCameraPreviewRequest())
                }
                it.getCapabilities().subscribe { result, _ ->
                    val current = System.currentTimeMillis()
                    Log.i("Capabilities", "Receive result with size ${result.previewResolutions.size} at $current, use time ${current - start}")
                    result.previewResolutions.forEach {
                        Log.i("resolution", it.toString())
                    }
                    latch.countDown()
                }
                latch.await()
                if (needStart) {
                    it.stopPreview()
                }
            }
            faceDetectCamera.restart()
        }
        faceDetectCamera.settingManager.configFor(faceDetectCamera.cameras[0].source.id, faceDetectCamera.cameras[0].id) {
            FotoCameraPreviewRequest(ResolutionSelector.MaxWidth)
        }
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
                    Logger.i(buildString {
                        if (detectResult.size == 1) {
                            val result = detectResult.mapNotNull { engine.searchFaceForScore(it.value) }
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
