package com.github.charleslzq.arcsofttools.kotlin

import android.graphics.Bitmap
import com.arcsoft.facerecognition.AFR_FSDKFace
import com.arcsoft.facerecognition.AFR_FSDKVersion
import com.github.charleslzq.faceengine.core.kotlin.store.FaceDataType
import com.github.charleslzq.faceengine.core.kotlin.store.Meta
import org.joda.time.LocalDateTime

/**
 * Created by charleslzq on 18-3-1.
 */
data class Person(
    override val id: String,
    val name: String,
    override val createTime: LocalDateTime = LocalDateTime.now(),
    override val updateTime: LocalDateTime = LocalDateTime.now()
) : Meta

data class Face(
    override val id: String,
    val pic: Bitmap,
    val data: AFR_FSDKFace,
    val version: AFR_FSDKVersion,
    override val createTime: LocalDateTime = LocalDateTime.now(),
    override val updateTime: LocalDateTime = LocalDateTime.now()
) : Meta

class ArcSoftFaceDataType :
    FaceDataType<Person, Face> {
    override val personClass: Class<Person>
        get() = Person::class.java
    override val faceClass: Class<Face>
        get() = Face::class.java
}