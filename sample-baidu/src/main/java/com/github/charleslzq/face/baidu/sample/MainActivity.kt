package com.github.charleslzq.face.baidu.sample

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import com.bin.david.form.data.column.Column
import com.bin.david.form.data.table.PageTableData
import com.github.charleslzq.face.baidu.BaiduFaceEngineService
import com.github.charleslzq.face.baidu.BaiduFaceEngineServiceBackground
import com.github.charleslzq.face.baidu.BaiduUserApi
import com.github.charleslzq.face.baidu.data.Image
import com.github.charleslzq.faceengine.support.runOnUI
import com.github.charleslzq.faceengine.support.toEncodedBytes
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.*

fun Context.toast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    runOnUI {
        Toast.makeText(this, message, duration).show()
    }
}

class MainActivity : AppCompatActivity() {

    private val connection = BaiduFaceEngineServiceBackground.getBuilder()
            .afterConnected { service ->
                Log.i("Main", "Service connected ${service.url}")
                baiduServerUrl.post {
                    baiduServerUrl.setText(service.url)
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
        refreshButton.setOnClickListener {
            refresh()
        }
        addGroupButton.setOnClickListener {
            if (connection.isConnected) {
                val dialogLayout = layoutInflater.inflate(R.layout.dialog_group_add, null)
                val input = dialogLayout.findViewById<EditText>(R.id.groupIdText)
                AlertDialog.Builder(this@MainActivity)
                        .setView(dialogLayout)
                        .setTitle("Input the group id")
                        .setPositiveButton("OK", null)
                        .setNegativeButton("CANCEL", { dialog, _ -> dialog.dismiss() })
                        .show()
                        .apply {
                            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                val groupId = input.text.toString()
                                if (groupId.length > 128) {
                                    input.error = "Group Id is too long, should be 128 byte at most"
                                } else if (!Regex(BAIDU_ID_REGEX).matches(groupId)) {
                                    input.error = "Illegal format"
                                } else if (groupId.isNotBlank()) {
                                    connection.instance!!.let { service ->
                                        launch(CommonPool) {
                                            service.addGroup(groupId).await()
                                            refresh()
                                        }
                                    }
                                    dismiss()
                                } else {
                                    input.error = "Group Id can not be empty"
                                }
                            }
                        }
            } else {
                toast("Service not connected")
            }
        }
        removeGroupButton.setOnClickListener {
            if (connection.isConnected) {
                val groupIdList = (userInfoTable.tableData as? PageTableData<TableItem>)?.t?.map { it.groupId }?.distinct()
                        ?: emptyList()
                if (groupIdList.isEmpty()) {
                    toast("There is no group to remove")
                } else {
                    val dialogLayout = layoutInflater.inflate(R.layout.dialog_group_remove, null)
                    val input = dialogLayout.findViewById<AutoCompleteTextView>(R.id.groupIdText)
                    input.setAdapter(ArrayAdapter<String>(
                            this@MainActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            groupIdList
                    ))
                    AlertDialog.Builder(this@MainActivity)
                            .setView(dialogLayout)
                            .setTitle("Input the group id")
                            .setPositiveButton("OK", null)
                            .setNegativeButton("CANCEL", { dialog, _ -> dialog.dismiss() })
                            .show()
                            .apply {
                                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                    val groupId = input.text.toString()
                                    if (groupIdList.contains(groupId)) {
                                        launch(CommonPool) {
                                            connection.instance!!.deleteGroup(groupId).await()
                                        }
                                        refresh()
                                        dismiss()
                                    } else {
                                        input.error = "Group Id Does not Exist"
                                    }
                                }
                            }
                }
            } else {
                toast("Service not connected")
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
                            refresh()
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun refresh() = launch(UI) {
        connection.instance?.let { service: BaiduFaceEngineService ->
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

    companion object {
        const val BAIDU_ID_REGEX = "[a-zA-Z0-9_]+"
    }
}