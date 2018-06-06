package com.github.charleslzq.face.baidu.sample

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import com.bin.david.form.data.column.Column
import com.bin.david.form.data.table.PageTableData
import com.github.charleslzq.face.baidu.BaiduFaceEngine
import com.github.charleslzq.face.baidu.BaiduFaceEngineServiceBackground
import com.github.charleslzq.face.baidu.BaiduUserApi
import com.github.charleslzq.face.baidu.data.Image
import com.github.charleslzq.faceengine.support.runOnUI
import com.github.charleslzq.faceengine.support.toEncodedBytes
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

fun Context.toast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    runOnUI {
        Toast.makeText(this, message, duration).show()
    }
}

class MainActivity : AppCompatActivity() {

    private val connection = BaiduFaceEngineServiceBackground.getBuilder()
            .afterConnected {
                Log.i("Main", "Service connected ${it.url}")
                baiduServerUrl.post {
                    baiduServerUrl.setText(it.url)
                }
            }.build()
    private val columns: List<Column<String>> = TableColumn.values().map { it.getColumnSetting() }
    private val timestamps = mutableListOf<Long>()

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
                                    connection.whenConnected {
                                        launch(CommonPool) {
                                            it.addGroup(groupId).await()
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
            connection.whenConnected { engine ->
                val groupIdList = getUserGroupsFromTable()
                if (groupIdList.isEmpty()) {
                    toast("There is no group to remove")
                } else {
                    val dialogLayout = layoutInflater.inflate(R.layout.dialog_group_remove, null)
                    val input = dialogLayout.findViewById<AutoCompleteTextView>(R.id.groupIdText)
                    input.setAdapter(getArrayAdapter(groupIdList))
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
                                            engine.deleteGroup(groupId).await()
                                        }
                                        refresh()
                                        dismiss()
                                    } else {
                                        input.error = "Group Id Does not Exist"
                                    }
                                }
                            }
                }
            }
        }
        copyUserButton.setOnClickListener {
            connection.whenConnected { engine ->
                val groupIdList = getUserGroupsFromTable()
                if (groupIdList.isEmpty()) {
                    toast("There is no group")
                } else {
                    val dialogLayout = layoutInflater.inflate(R.layout.dialog_group_user_copy, null)
                    val srcGroup = dialogLayout.findViewById<AutoCompleteTextView>(R.id.srcGroupIdText)
                    val dstGroup = dialogLayout.findViewById<AutoCompleteTextView>(R.id.dstGroupIdText)
                    val user = dialogLayout.findViewById<AutoCompleteTextView>(R.id.userIdText)
                    srcGroup.setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            srcGroup.setAdapter(getArrayAdapter(groupIdList.filter { it != dstGroup.text.toString() }))
                        } else {
                            if (!groupIdList.contains(srcGroup.text.toString())) {
                                srcGroup.error = "Group Id Does not Exist"
                            }
                        }
                    }
                    dstGroup.setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            dstGroup.setAdapter(getArrayAdapter(groupIdList.filter { it != srcGroup.text.toString() }))
                        } else {
                            if (!groupIdList.contains(srcGroup.text.toString())) {
                                dstGroup.error = "Group Id Does not Exist"
                            }
                        }
                    }
                    user.setOnFocusChangeListener { _, hasFocus ->
                        val userIdList = if (groupIdList.contains(srcGroup.text.toString())) {
                            getUserListFromTable(srcGroup.text.toString())
                        } else {
                            emptyList()
                        }
                        if (hasFocus) {
                            user.setAdapter(getArrayAdapter(userIdList))
                        } else {
                            if (!userIdList.contains(user.text.toString())) {
                                user.error = "User Id Does not Exist in source Group"
                            }
                        }
                    }
                    AlertDialog.Builder(this@MainActivity)
                            .setView(dialogLayout)
                            .setTitle("Input the Information")
                            .setPositiveButton("OK", null)
                            .setNegativeButton("CANCEL", { dialog, _ -> dialog.dismiss() })
                            .show()
                            .apply {
                                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                    val srcGroupId = srcGroup.text.toString()
                                    val dstGroupId = dstGroup.text.toString()
                                    val userId = user.text.toString()

                                    if (groupIdList.contains(srcGroupId) && groupIdList.contains(dstGroupId)) {
                                        launch(CommonPool) { engine.copyUser(dstGroupId, srcGroupId, userId).await() }
                                        refresh()
                                        dismiss()
                                    } else {
                                        toast("Group Id Does not Exist")
                                    }
                                }
                            }
                }
            }
        }
        addUserButton.setOnClickListener {
            startActivityForResult(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    RequestCodes.IMAGE_CAMERA.code
            )
        }
        imageSearchButton.setOnClickListener {
            timestamps.add(System.currentTimeMillis())
            startActivityForResult(
                    Intent(this, FaceDetectActivity::class.java),
                    RequestCodes.FACE_CHECK.code
            )
        }
        connection.bind<BaiduFaceEngineServiceBackground>(this)
    }

    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && connection.isConnected && data != null) {
            connection.whenConnected { engine ->
                when (RequestCodes.fromCode(requestCode)) {
                    RequestCodes.IMAGE_CAMERA -> launch(UI) {
                        val image = data.extras["data"] as? Bitmap
                        if (image != null) {
                            val groupIdList = getUserGroupsFromTable()
                            if (groupIdList.isEmpty()) {
                                toast("There is no group")
                            } else {
                                val dialogLayout = layoutInflater.inflate(R.layout.dialog_user_add, null)
                                val group = dialogLayout.findViewById<AutoCompleteTextView>(R.id.groupIdText)
                                group.setAdapter(getArrayAdapter(groupIdList))
                                group.setOnFocusChangeListener { _, hasFocus ->
                                    if (!hasFocus && !groupIdList.contains(group.text.toString())) {
                                        group.error = "Group Id Does not Exist"
                                    }
                                }
                                val user = dialogLayout.findViewById<AutoCompleteTextView>(R.id.userIdText)
                                user.setOnFocusChangeListener { _, hasFocus ->
                                    if (hasFocus) {
                                        if (groupIdList.contains(group.text.toString())) {
                                            user.setAdapter(getArrayAdapter(getUserListFromTable(group.text.toString())))
                                        }
                                    }
                                }
                                val userInfo = dialogLayout.findViewById<EditText>(R.id.userInfoText)
                                AlertDialog.Builder(this@MainActivity)
                                        .setView(dialogLayout)
                                        .setTitle("Input the Information")
                                        .setPositiveButton("OK", null)
                                        .setNegativeButton("CANCEL", { dialog, _ -> dialog.dismiss() })
                                        .show()
                                        .apply {
                                            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                                val groupId = group.text.toString()
                                                val userId = user.text.toString()
                                                val userInfoText = userInfo.text.toString()

                                                if (groupIdList.contains(groupId)) {
                                                    launch(CommonPool) {
                                                        engine.addUser(groupId, BaiduUserApi.RegisterImage(
                                                                Image(Image.Type.BASE64, toEncodedBytes(image)),
                                                                userId, userInfoText
                                                        )).await()
                                                    }
                                                    refresh()
                                                    dismiss()
                                                } else {
                                                    toast("Group Id Does not Exist")
                                                }
                                            }
                                        }
                            }
                        } else {
                            toast("Fail to get Image")
                        }
                    }
                    RequestCodes.FACE_CHECK -> {
                        val groupId = data.extras["groupId"] as String
                        val userId = data.extras["userId"] as String
                        val userInfo = data.extras["userInfo"] as? String?
                        toast(buildString {
                            append("Found User with Group ID $groupId and User ID $userId")
                            userInfo?.let { append(", User Info $userInfo") }
                            val current = System.currentTimeMillis()
                            append(", Use Time: ${current - timestamps.last()}")
                        }.also {
                            Log.i("SEARCH", it)
                        })
                    }
                }
            }
        }
    }

    private fun getArrayAdapter(idList: List<String>) = ArrayAdapter<String>(
            this@MainActivity,
            android.R.layout.simple_dropdown_item_1line,
            idList
    )

    private fun getUserGroupsFromTable() = (userInfoTable.tableData as? PageTableData<TableItem>)?.t?.map { it.groupId }?.distinct()
            ?: emptyList()

    private fun getUserListFromTable(groupId: String? = null) =
            (userInfoTable.tableData as? PageTableData<TableItem>)?.t?.filter { groupId == null || it.groupId == groupId }?.map { it.userId }
                    ?: emptyList()

    fun refresh() = launch(UI) {
        connection.whenConnected { engine: BaiduFaceEngine ->
            engine.setUrlWithCallback(baiduServerUrl.text.toString()) {
                launch(UI) {
                    val dataList = mutableListOf<TableItem>()
                    try {
                        Log.i("Main", "try to listGroup from ${engine.url}")
                        val groupIdList = engine.listGroup().await().result?.groupIdList
                                ?: emptyList()
                        engine.defaultSearchOption = engine.defaultSearchOption.copy(
                                groups = groupIdList
                        )
                        groupIdList.forEach { groupId ->
                            val userIdList = engine.listUser(groupId).await().result?.userIdList
                            if (userIdList == null || userIdList.isEmpty()) {
                                dataList.add(TableItem(groupId))
                            } else {
                                userIdList.forEach { userId ->
                                    val faceList = engine.listFace(groupId, userId).await().result?.faceList
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