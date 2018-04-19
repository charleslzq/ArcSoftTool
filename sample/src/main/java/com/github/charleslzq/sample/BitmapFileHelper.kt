package com.github.charleslzq.sample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.util.*

object BitmapFileHelper {
    const val directory = "/tmp_pic_taken/"

    fun save(bitmap: Bitmap): String {
        val fileName = UUID.randomUUID().toString()
        getFile(fileName).run {
            parentFile.mkdirs()
            FileOutputStream(this).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        return fileName
    }

    fun load(fileName: String, delete: Boolean = true): Bitmap {
        return BitmapFactory.decodeFile(getFile(fileName).absolutePath).also {
            if (delete) {
                getFile(fileName).delete()
            }
        }
    }

    private fun getFile(fileName: String) = File(Environment.getExternalStorageDirectory().absolutePath + directory + fileName)
}