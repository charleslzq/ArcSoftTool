@file:JvmName("ImageUtils")

package com.github.charleslzq.faceengine.support

import android.graphics.*
import android.util.Base64
import com.github.charleslzq.faceengine.view.CameraPreview
import java.io.ByteArrayOutputStream
import java.net.URLEncoder

/**
 * Created by charleslzq on 18-3-8.
 */
fun toBitmap(frame: CameraPreview.PreviewFrame) =
        YuvImage(frame.image, ImageFormat.NV21, frame.size.width, frame.size.height, null).run {
            TempByteArrayOutputStream().use {
                compressToJpeg(Rect(0, 0, frame.size.width, frame.size.height), 100, it)
                BitmapFactory.decodeByteArray(it.data, 0, it.data.size)
            }
        }

fun toEncodedBytes(frame: CameraPreview.PreviewFrame) = toBitmap(frame).run {
    ByteArrayOutputStream().let {
        compress(Bitmap.CompressFormat.PNG, 100, it)
        URLEncoder.encode(Base64.encodeToString(it.toByteArray(), Base64.DEFAULT), Charsets.UTF_8.name())
    }
}

private class TempByteArrayOutputStream : ByteArrayOutputStream() {
    val data: ByteArray
        get() = super.buf
}