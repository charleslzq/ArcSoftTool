@file:JvmName("Nv21ImageUtils")

package com.github.charleslzq.arcsofttools.kotlin.support

import android.graphics.Bitmap
import com.guo.android_extend.image.ImageConverter
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame

/**
 * Created by charleslzq on 18-3-14.
 */
fun toNv21Bytes(image: Bitmap) = ByteArray(image.width * image.height * 3 / 2).apply {
    ImageConverter().run {
        initial(image.width, image.height, ImageConverter.CP_PAF_NV21)
        convert(image, this@apply)
        destroy()
    }
}

@JvmOverloads
fun toFrame(image: Bitmap, toBytes: (Bitmap) -> ByteArray = ::toNv21Bytes) = Frame(Resolution(image.width, image.height), toBytes(image), 0)
