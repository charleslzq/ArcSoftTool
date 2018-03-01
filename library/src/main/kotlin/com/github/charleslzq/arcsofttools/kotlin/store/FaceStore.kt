package com.github.charleslzq.arcsofttools.kotlin.store

import android.graphics.Bitmap
import com.arcsoft.facerecognition.AFR_FSDKFace
import com.arcsoft.facerecognition.AFR_FSDKVersion
import org.joda.time.LocalDateTime

/**
 * Created by charleslzq on 18-2-28.
 */
data class Person(
    val id: String,
    val name: String,
    val createTime: LocalDateTime = LocalDateTime.now(),
    val updateTime: LocalDateTime = LocalDateTime.now()
)

data class Face(
    val id: String,
    val pic: Bitmap,
    val data: AFR_FSDKFace,
    val version: AFR_FSDKVersion,
    val createTime: LocalDateTime = LocalDateTime.now(),
    val updateTime: LocalDateTime = LocalDateTime.now()
)

data class FaceData(val person: Person, val faces: List<Face> = emptyList())

interface ReadOnlyFaceStore {
    fun getPersonIds(): List<String>
    fun getFaceData(personId: String): FaceData?
    fun getPerson(personId: String): Person?
    fun getFaceIdList(personId: String): List<String>
    fun getFace(personId: String, faceId: String): Face?
}

interface ReadWriteFaceStore : ReadOnlyFaceStore {
    fun savePerson(person: Person)
    fun saveFace(personId: String, face: Face)
    fun saveFaceData(faceData: FaceData)
    fun deleteFaceData(personId: String)
    fun deleteFace(personId: String, faceId: String)
    fun clearFace(personId: String)
}

interface FaceStoreChangeListener {
    fun onPersonUpdate(person: Person)
    fun onFaceUpdate(personId: String, face: Face)
    fun onFaceDataDelete(personId: String)
    fun onFaceDelete(personId: String, faceId: String)
    fun onPersonFaceClear(personId: String)
}