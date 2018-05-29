package com.github.charleslzq.face.baidu.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.bin.david.form.data.column.Column
import com.bin.david.form.data.table.PageTableData
import com.github.charleslzq.face.baidu.BaiduFaceEngineService
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
                    refresh(service)
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
}