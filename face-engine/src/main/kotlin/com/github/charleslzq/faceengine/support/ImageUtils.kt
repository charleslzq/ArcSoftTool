@file:JvmName("ImageUtils")

package com.github.charleslzq.faceengine.support

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import com.github.charleslzq.faceengine.view.CameraPreview
import java.io.ByteArrayOutputStream

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

private class TempByteArrayOutputStream : ByteArrayOutputStream() {
    val data: ByteArray
        get() = super.buf
}