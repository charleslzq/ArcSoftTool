package com.github.charleslzq.sample

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.bin.david.form.data.column.Column
import com.bin.david.form.data.table.PageTableData
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceEngineService
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftEngineService
import com.github.charleslzq.arcsofttools.kotlin.support.toFrame
import com.github.charleslzq.facestore.FaceData
import com.github.charleslzq.facestore.FaceStoreChangeListener
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.format.DateTimeFormat
import java.util.*


fun Context.toast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(this, message, duration).show()

class MainActivity : AppCompatActivity() {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            faceEngineService!!.store.apply {
                listeners.remove(storeListener)
            }
            faceEngineService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            @Suppress("UNCHECKED_CAST")
            faceEngineService =
                    service as ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>>
            faceEngineService!!.store.apply {
                listeners.add(storeListener)
                refresh()
            }
            reload(100)
        }

    }
    private var faceEngineService: ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>>? =
            null
    private val fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val columns: List<Column<*>> = listOf(
            Column("Person",
                    Column<String>("id", "person.id"),
                    Column<String>("name", "person.name")
            ),
            Column<List<Face>>("Face Count", "faces") {
                it.size.toString()
            },
            Column<List<Face>>("Last Update", "faces") {
                it.map { it.updateTime }.max()?.let { fmt.print(it) } ?: "UNKNOWN"
            }
    )
    private val storeListener = object : FaceStoreChangeListener<Person, Face> {
        override fun onPersonUpdate(person: Person) {
            reload()
        }

        override fun onFaceUpdate(personId: String, face: Face) {
            reload()
        }

        override fun onFaceDataDelete(personId: String) {
            reload()
        }

        override fun onFaceDelete(personId: String, faceId: String) {
            reload()
        }

        override fun onPersonFaceClear(personId: String) {
            reload()
        }
    }
    private val defaultFilter: (FaceData<Person, Face>) -> Boolean = { true }
    private var tableFilter = defaultFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        faceStoreTable.config.apply {
            isShowXSequence = false
            isShowYSequence = false
        }
        tableFilterText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.trim()?.run {
                    tableFilter = if (isBlank()) {
                        defaultFilter
                    } else {
                        {
                            it.person.id.contains(this) || it.person.name.contains(this)
                        }
                    }
                    if (!reload()) {
                        toast("Unable to find corresponding person with id or name contains the text")
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })
        captureImageButton.setOnClickListener {
            startActivityForResult(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    RequestCodes.IMAGE_CAMERA.code
            )
        }
        checkFaceButton.setOnClickListener {
            startActivityForResult(
                    Intent(this, FaceDetectActivity::class.java),
                    RequestCodes.FACE_CHECK.code
            )
        }
        refreshButton.setOnClickListener {
            faceEngineService?.store?.refresh()
        }
        bindService(
                Intent(this, WebSocketArcSoftEngineService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (RequestCodes.fromCode(requestCode)) {
            RequestCodes.IMAGE_CAMERA -> if (resultCode == Activity.RESULT_OK && data != null && faceEngineService != null) {
                faceEngineService!!.detect(toFrame(data.extras["data"] as Bitmap)).run {
                    if (isNotEmpty() && size == 1) {
                        forEach {
                            var selectedPerson: SimplePerson? = null
                            val dialogLayout = layoutInflater.inflate(R.layout.dialog_register, null)
                            val autoCompleteTexts = dialogLayout.findViewById<AutoCompleteTextView>(R.id.personRegister)
                            autoCompleteTexts.setAdapter(ArrayAdapter<SimplePerson>(
                                    this@MainActivity,
                                    android.R.layout.simple_dropdown_item_1line,
                                    faceEngineService!!.engine.store.getPersonIds()
                                            .mapNotNull { faceEngineService!!.engine.store.getPerson(it) }
                                            .map { SimplePerson.fromPerson(it) }
                            ))
                            autoCompleteTexts.setOnItemClickListener { parent, _, position, _ ->
                                @Suppress("UNCHECKED_CAST")
                                selectedPerson = (parent.adapter as ArrayAdapter<SimplePerson>).getItem(position)
                            }
                            autoCompleteTexts.addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                                }

                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                }

                                override fun afterTextChanged(s: Editable?) {
                                    selectedPerson = null
                                }
                            })
                            AlertDialog.Builder(this@MainActivity)
                                    .setView(dialogLayout)
                                    .setTitle("Your ID or Name")
                                    .setPositiveButton("OK", { _, _ ->
                                        val personId = selectedPerson?.id
                                                ?: UUID.randomUUID().toString()
                                        val personName = selectedPerson?.name
                                                ?: autoCompleteTexts.text.toString()
                                        if (selectedPerson == null) {
                                            faceEngineService!!.store.savePerson(Person(personId, personName))
                                        }
                                        faceEngineService!!.store.saveFace(personId, it)
                                    })
                                    .setNegativeButton("CANCEL", { dialog, _ -> dialog.dismiss() })
                                    .show()
                        }
                    }
                }
            }
            RequestCodes.FACE_CHECK -> if (resultCode == Activity.RESULT_OK && data != null) {
                val personName = data.extras["personName"] as String
                toast("Found Person $personName")
            } else if (resultCode == Activity.RESULT_CANCELED) {
                toast("Fail to identify face")
            }
        }
    }

    private fun reload(newPageSize: Int? = null): Boolean =
        faceEngineService?.store?.run {
            getPersonIds().mapNotNull { getFaceData(it) }.filter(tableFilter).takeIf { it.isNotEmpty() }?.let {
                faceStoreTable.tableData = PageTableData<FaceData<Person, Face>>("Registered Persons And Faces",
                        it,
                        columns
                ).apply {
                    newPageSize?.let { pageSize = it }
                }
                true
            } ?: false
        } ?: false

    enum class RequestCodes {
        IMAGE_CAMERA,
        FACE_CHECK;

        val code = ordinal + 1

        companion object {
            @JvmStatic
            fun fromCode(requestCode: Int) = RequestCodes.values()[requestCode - 1]
        }
    }

    data class SimplePerson(
            val id: String,
            val name: String
    ) {
        override fun toString() = buildString {
            append(name)
            append("#")
            append(id)
        }

        companion object {
            @JvmStatic
            fun fromPerson(person: Person) = SimplePerson(person.id, person.name)
        }
    }
}
