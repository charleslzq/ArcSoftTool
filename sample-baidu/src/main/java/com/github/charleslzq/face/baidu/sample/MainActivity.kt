package com.github.charleslzq.face.baidu.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.charleslzq.face.baidu.BaiduFaceEngineServiceBackground
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {

    private val connection = BaiduFaceEngineServiceBackground.getBuilder()
            .afterConnected { service ->
                Log.i("Main", "Service connected ${service.url}")
                baiduServerUrl.post {
                    baiduServerUrl.setText(service.url)
                }
                resetButton.setOnClickListener {
                    service.setUrlWithCallback(baiduServerUrl.text.toString()) {
                        launch(UI) {
                            try {
                                Log.i("Main", "try to list from ${service.url}")
                                groups.text = service.list().await().result?.groupIdList?.joinToString(",") ?: "404 NOT FOUND"
                            } catch (throwable: Throwable) {
                                Log.e("Main", "Error happened", throwable)
                            }
                        }
                    }
                }
            }.build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindService(
                Intent(this, BaiduFaceEngineServiceBackground::class.java),
                connection,
                Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
    }
}