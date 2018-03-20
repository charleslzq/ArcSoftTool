package com.github.charleslzq.faceengine.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.fatboyindustrial.gsonjodatime.Converters
import com.google.gson.*
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type


/**
 * Created by charleslzq on 18-3-20.
 */
class BitmapConverter : JsonSerializer<Bitmap>, JsonDeserializer<Bitmap> {
    override fun serialize(src: Bitmap, typeOfSrc: Type?, context: JsonSerializationContext?) =
            ByteArrayOutputStream().let {
                src.compress(Bitmap.CompressFormat.PNG, 100, it)
                JsonPrimitive(Base64.encodeToString(it.toByteArray(), Base64.DEFAULT))
            }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?) =
            Base64.decode(json.asString, Base64.DEFAULT).let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }

    companion object {
        fun createGson() = Converters.registerLocalDateTime(
                GsonBuilder().registerTypeAdapter(Bitmap::class.java, BitmapConverter())
        ).create()
    }
}