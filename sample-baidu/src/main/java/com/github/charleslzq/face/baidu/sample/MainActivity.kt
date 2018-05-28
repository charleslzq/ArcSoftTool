package com.github.charleslzq.face.baidu.sample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.charleslzq.face.baidu.BaiduFaceEngineService
import com.github.charleslzq.face.baidu.BaiduFaceEngineServiceBackground
import com.github.charleslzq.faceengine.support.runOnIo
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            faceEngineService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            @Suppress("UNCHECKED_CAST")
            faceEngineService = service as BaiduFaceEngineService
            Log.i("Main", "Service connected ${faceEngineService!!.url}")
            baiduServerUrl.post {
                baiduServerUrl.setText(faceEngineService!!.url)
            }
            resetButton.setOnClickListener {
                faceEngineService!!.setUrlWithCallback(baiduServerUrl.text.toString()) {
                    runOnIo {
                        try {
                            Log.i("Main", "try to list from ${faceEngineService!!.url}")
                            faceEngineService!!.list().execute().takeIf { it.isSuccessful }?.body()?.let {
                                groups.post {
                                    groups.text = it.result?.groupIdList?.joinToString(",") ?: "UN-FOUND"
                                }
                            }
                        } catch (throwable: Throwable) {

                        }
                    }
                }
            }
        }

    }
    private var faceEngineService: BaiduFaceEngineService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindService(
                Intent(this, BaiduFaceEngineServiceBackground::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }
}