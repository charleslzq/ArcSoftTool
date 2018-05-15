package com.github.charleslzq.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.charleslzq.faceengine.support.toBitmap
import kotlinx.android.synthetic.main.activity_face_register.*
import java.util.concurrent.atomic.AtomicBoolean

class FaceRegisterActivity : AppCompatActivity() {
    private var requireTakePicture = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_register)
        faceRegisterCamera.setOnClickListener {
            requireTakePicture.compareAndSet(false, true)
        }
        faceRegisterCamera.onPreview {
            if (requireTakePicture.compareAndSet(true, false)) {
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("picPath", BitmapFileHelper.save(toBitmap(it)))
                })
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        faceRegisterCamera.start()
    }

    override fun onPause() {
        faceRegisterCamera.pause()
        super.onPause()
    }

    override fun onDestroy() {
        faceRegisterCamera.close()
        super.onDestroy()
    }
}
