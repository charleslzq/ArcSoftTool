package com.github.charleslzq.face.baidu.sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.bin.david.form.data.column.Column
import com.bin.david.form.data.table.PageTableData
import com.github.charleslzq.face.baidu.BaiduFaceEngineService
import com.github.charleslzq.face.baidu.BaiduFaceEngineServiceBackground
import com.github.charleslzq.face.baidu.BaiduUserApi
import com.github.charleslzq.face.baidu.data.Image
import com.github.charleslzq.faceengine.support.toEncodedBytes
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private val connection = BaiduFaceEngineServiceBackground.getBuilder()
            .afterConnected { service ->
                Log.i("Main", "Service connected ${service.url}")
                baiduServerUrl.post {
                    baiduServerUrl.setText(service.url)
                }
                resetButton.setOnClickListener {
                    refresh(service)
                }
                addFab.setOnClickListener {
                    startActivityForResult(
                            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                            RequestCodes.IMAGE_CAMERA.code
                    )
//                    launch(UI) {
//                        val groupId = UUID.randomUUID().toString().toUpperCase().replace('-', '_')
//                        service.addGroup(groupId).await()
//                        refresh(service)
//                    }
                }
                removeFab.setOnClickListener {
                    launch(UI) {
                        service.listGroup().await().result?.groupIdList?.lastOrNull()?.let {
                            service.deleteGroup(it)
                        }
                        refresh(service)
                    }
                }
            }.build()
    private val columns: List<Column<String>> = TableColumn.values().map { it.getColumnSetting() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userInfoTable.apply {
            config.apply {
                isShowXSequence = false
                isShowYSequence = false
            }
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && connection.isConnected && data != null) {
            connection.instance!!.let { service ->
                when (RequestCodes.fromCode(requestCode)) {
                    RequestCodes.IMAGE_CAMERA -> launch(UI) {
                        val image = data.extras["data"] as? Bitmap
                        if (image != null) {
                            val groupId = UUID.randomUUID().toString().toUpperCase().replace('-', '_')
                            service.addGroup(groupId).await()
                            val userId = UUID.randomUUID().toString().toUpperCase().replace('-', '_')
                            val userInfo = "Created automatically"
                            val result = service.addUser(groupId, BaiduUserApi.RegisterImage(
                                    Image(Image.Type.BASE64, toEncodedBytes(image)),
                                    userId, userInfo
                            )).await()
                            print(result.toString())
                            refresh(service)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun refresh(service: BaiduFaceEngineService) = launch(UI) {
        service.setUrlWithCallback(baiduServerUrl.text.toString()) {
            launch(UI) {
                val dataList = mutableListOf<TableItem>()
                try {
                    Log.i("Main", "try to listGroup from ${service.url}")
                    val groupIdList = service.listGroup().await().result?.groupIdList
                    groupIdList?.forEach { groupId ->
                        val userIdList = service.listUser(groupId).await().result?.userIdList
                        if (userIdList == null || userIdList.isEmpty()) {
                            dataList.add(TableItem(groupId))
                        } else {
                            userIdList.forEach { userId ->
                                val faceList = service.listFace(groupId, userId).await().result?.faceList
                                if (faceList == null || faceList.isEmpty()) {
                                    dataList.add(TableItem(groupId, userId))
                                } else {
                                    faceList.forEach {
                                        dataList.add(TableItem(groupId, userId, it.faceToken, it.createTime))
                                    }
                                }
                            }
                        }
                    }
                } catch (throwable: Throwable) {
                    Log.e("Main", "Error happened", throwable)
                }
                userInfoTable.tableData = PageTableData<TableItem>(
                        "Registered Faces",
                        dataList,
                        columns
                )
            }
        }
    }

    data class TableItem(
            val groupId: String,
            val userId: String = "",
            val faceToken: String = "",
            val createTime: String = ""
    )

    enum class TableColumn(
            val title: String,
            val field: String
    ) {
        GROUP_ID("group", "groupId"),
        USER_ID("userId", "userId"),
        FACE_TOKEN("faceToken", "faceToken"),
        CREATE_TIME("createTime", "createTime");

        fun getColumnSetting() = Column<String>(title, field).apply {
            isAutoMerge = true
        }
    }

    enum class RequestCodes {
        IMAGE_CAMERA,
        FACE_CHECK;

        val code = ordinal + 1

        companion object {
            @JvmStatic
            fun fromCode(requestCode: Int) = RequestCodes.values()[requestCode - 1]
        }
    }
}