@file:JvmName("ImageUtils")

package com.github.charleslzq.faceengine.support

import android.graphics.*
import android.util.Base64
import com.github.charleslzq.faceengine.view.PreviewFrame
import java.io.ByteArrayOutputStream

/**
 * Created by charleslzq on 18-3-8.
 */
fun toBitmap(frame: PreviewFrame): Bitmap =
        YuvImage(frame.image, ImageFormat.NV21, frame.size.width, frame.size.height, null).run {
            TempByteArrayOutputStream().use {
                compressToJpeg(Rect(0, 0, frame.size.width, frame.size.height), 100, it)
                BitmapFactory.decodeByteArray(it.data, 0, it.data.size)
            }
        }

fun toEncodedBytes(bitmap: Bitmap): String = bitmap.run {
    ByteArrayOutputStream().let {
        compress(Bitmap.CompressFormat.PNG, 100, it)
        Base64.encodeToString(it.toByteArray(), Base64.DEFAULT)
    }
}

fun toEncodedBytes(frame: PreviewFrame) = toEncodedBytes(toBitmap(frame))

private class TempByteArrayOutputStream : ByteArrayOutputStream() {
    val data: ByteArray
        get() = super.buf
}