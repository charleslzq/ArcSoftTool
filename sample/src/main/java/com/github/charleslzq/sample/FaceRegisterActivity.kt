package com.github.charleslzq.sample

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceEngineService
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftEngineService
import com.github.charleslzq.faceengine.support.faceEngineTaskExecutor
import com.github.charleslzq.faceengine.support.toBitmap
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore
import kotlinx.android.synthetic.main.activity_face_register.*
import java.util.concurrent.atomic.AtomicBoolean

class FaceRegisterActivity : AppCompatActivity() {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            faceEngineService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            @Suppress("UNCHECKED_CAST")
            faceEngineService =
                    service as ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>>
        }

    }
    private var faceEngineService: ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>>? =
            null
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
        bindService(
                Intent(this, WebSocketArcSoftEngineService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        )
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
        unbindService(serviceConnection)
        faceRegisterCamera.close()
        faceEngineTaskExecutor.cancelTasks()
        super.onDestroy()
    }
}
