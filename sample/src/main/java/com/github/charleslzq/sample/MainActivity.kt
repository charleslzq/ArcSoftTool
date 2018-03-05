package com.github.charleslzq.sample

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftEngineAdapter
import com.github.charleslzq.arcsofttools.kotlin.Person
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val engine = ArcSoftEngineAdapter.createEngine(resources)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        captureImageButton.setOnClickListener {
            startActivityForResult(
                Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                REQUEST_CODE_IMAGE_CAMERA
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_IMAGE_CAMERA && resultCode == Activity.RESULT_OK) {
            engine.detect(data.extras["data"] as Bitmap).run {
                if (isNotEmpty()) {
                    engine.store.savePerson(Person("test", "test_name"))
                    forEach {
                        engine.store.saveFace("test", it)
                    }
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_CAMERA = 1
        private const val REQUEST_CODE_IMAGE_OP = 2
        private const val REQUEST_CODE_OP = 3
    }
}
