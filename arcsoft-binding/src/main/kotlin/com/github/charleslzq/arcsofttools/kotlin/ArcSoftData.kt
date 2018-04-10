package com.github.charleslzq.arcsofttools.kotlin

import android.graphics.Bitmap
import android.graphics.Rect
import com.arcsoft.facerecognition.AFR_FSDKFace
import com.arcsoft.facerecognition.AFR_FSDKVersion
import com.github.charleslzq.facestore.Meta
import org.joda.time.LocalDateTime

/**
 * Created by charleslzq on 18-3-1.
 */
data class Person
@JvmOverloads
constructor(
        override val id: String,
        val name: String,
        override val createTime: LocalDateTime = LocalDateTime.now(),
        override val updateTime: LocalDateTime = LocalDateTime.now()
) : Meta

data class Face
@JvmOverloads
constructor(
        override val id: String,
        val pic: Bitmap,
        val data: AFR_FSDKFace,
        val version: AFR_FSDKVersion,
        override val createTime: LocalDateTime = LocalDateTime.now(),
        override val updateTime: LocalDateTime = LocalDateTime.now()
) : Meta

data class FaceLocation(
        val rect: Rect,
        val degree: Int
)

data class DetectedAge(
        val faceLocation: FaceLocation,
        val age: Int
)

data class DetectedGender(
        val faceLocation: FaceLocation,
        val gender: ArcSoftGender
)

enum class ArcSoftGender {
    UNKNOWN,
    MALE,
    FEMALE;

    val code = ordinal - 1

    companion object {
        @JvmStatic
        fun fromCode(code: Int) = ArcSoftGender.values()[code + 1]
    }
}