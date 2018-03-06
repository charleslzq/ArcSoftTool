package com.github.charleslzq.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_face_detect.*

class FaceDetectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.autoTakePictureCallback.publisher.observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                counter.text = faceDetectCamera.count.toString()
                resultDisplay.setImageBitmap(it)
            }
    }

    override fun onResume() {
        super.onResume()
        faceDetectCamera.start()
    }

    override fun onPause() {
        faceDetectCamera.stop()
        super.onPause()
    }
}
