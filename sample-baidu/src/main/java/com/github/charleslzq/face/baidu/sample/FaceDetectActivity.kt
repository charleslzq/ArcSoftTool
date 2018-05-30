package com.github.charleslzq.face.baidu.sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.charleslzq.face.baidu.BaiduFaceEngineServiceBackground
import com.github.charleslzq.face.baidu.toImage
import kotlinx.android.synthetic.main.activity_face_detect.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class FaceDetectActivity : AppCompatActivity() {

    private val connection = BaiduFaceEngineServiceBackground.getBuilder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detect)
        faceDetectCamera.onPreview(200000) { frame ->
            //            connection.instance?.search(frame)?.let {
//                setResult(Activity.RESULT_OK, Intent().apply {
//                    putExtra("groupId", it.groupId)
//                    putExtra("userId", it.userId)
//                    putExtra("userInfo", it.userInfo)
//                })
//                finish()
//            }
            connection.instance?.let { service ->
                launch(CommonPool) {
                    val groupResponse = service.listGroup().await()
                    Log.i("SEARCH", groupResponse.toString())
                    val groupList = groupResponse.result?.groupIdList ?: emptyList()
                    if (groupList.isNotEmpty()) {
                        val searchResult = service.search(frame.toImage(), groupList.toTypedArray()).await()
                        Log.i("SEARCH", searchResult.toString())
                        val users = searchResult.result?.userList ?: emptyList()
                        users.maxBy { it.score }?.let {
                            setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra("groupId", it.groupId)
                                putExtra("userId", it.userId)
                                putExtra("userInfo", it.userInfo)
                            })
                            finish()
                        }
                    }
                }
            }
        }
        bindService(
                Intent(this, BaiduFaceEngineServiceBackground::class.java),
                connection,
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
        unbindService(connection)
        faceDetectCamera.close()
        super.onDestroy()
    }
}
