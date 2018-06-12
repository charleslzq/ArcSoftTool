package com.github.charleslzq.faceengine.view.config

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.gson.GsonBuilder
import java.io.FileInputStream
import java.io.FileOutputStream

internal val previewRequestTypeAdapterFactory = RuntimeTypeAdapterFactory.of(CameraPreviewRequest::class.java, "type")
        .registerSubtype(FotoCameraPreviewRequest::class.java, "FOTO")
        .registerSubtype(UVCCameraPreviewRequest::class.java, "UVC")
internal val resolutionSelectorTypeAdapterFactory = RuntimeTypeAdapterFactory.of(ResolutionSelector::class.java, "type")
        .registerSubtype(ResolutionSelector.MaxHeight::class.java, "MaxHeight")
        .registerSubtype(ResolutionSelector.MaxWidth::class.java, "MaxWidth")
        .registerSubtype(ResolutionSelector.MaxArea::class.java, "MaxArea")
        .registerSubtype(ResolutionSelector.MinHeight::class.java, "MinHeight")
        .registerSubtype(ResolutionSelector.MinWidth::class.java, "MinWidth")
        .registerSubtype(ResolutionSelector.MinArea::class.java, "MinArea")
        .registerSubtype(ResolutionSelector.Match::class.java, "Match")

internal class SettingStore(val context: Context) {
    private val gson = GsonBuilder()
            .registerTypeAdapterFactory(previewRequestTypeAdapterFactory)
            .registerTypeAdapterFactory(resolutionSelectorTypeAdapterFactory)
            .create()

    fun load(): CameraSetting = openForInput {
        it.bufferedReader().use {
            gson.fromJson(it.readText(), CameraSetting::class.java)
        }
    }

    fun store(cameraSetting: CameraSetting) {
        openForOutput {
            it.bufferedWriter().use {
                it.write(gson.toJson(cameraSetting))
            }
        }
    }

    private fun fileExist() = context.getFileStreamPath(FILE_NAME).let {
        it != null && it.exists()
    }

    private fun <R> openForInput(process: (FileInputStream) -> R): R {
        if (!fileExist()) {
            store(CameraSetting(CameraPreviewConfiguration(), emptyList()))
        }
        return context.openFileInput(FILE_NAME).use(process)
    }

    private fun <R> openForOutput(process: (FileOutputStream) -> R) = context.openFileOutput(FILE_NAME, MODE_PRIVATE).use(process)

    companion object {
        const val FILE_NAME = "setting.json"
    }
}