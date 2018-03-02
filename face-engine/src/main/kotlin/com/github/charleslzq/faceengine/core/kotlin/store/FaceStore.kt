package com.github.charleslzq.faceengine.core.kotlin.store

import org.joda.time.LocalDateTime

/**
 * Created by charleslzq on 18-2-28.
 */
interface Meta {
    val id: String
    val createTime: LocalDateTime
    val updateTime: LocalDateTime
}

data class FaceData<out P : Meta, out F : Meta>(val person: P, val faces: List<F> = emptyList())

interface FaceDataType<P : Meta, F : Meta> {
    val personClass: Class<P>
    val faceClass: Class<F>
}

interface ReadOnlyFaceStore<out P : Meta, out F : Meta> {
    fun getPersonIds(): List<String>
    fun getFaceData(personId: String): FaceData<P, F>?
    fun getPerson(personId: String): P?
    fun getFaceIdList(personId: String): List<String>
    fun getFace(personId: String, faceId: String): F?
}

interface ReadWriteFaceStore<P : Meta, F : Meta> : ReadOnlyFaceStore<P, F> {
    fun savePerson(person: P)
    fun saveFace(personId: String, face: F)
    fun saveFaceData(faceData: FaceData<P, F>)
    fun deleteFaceData(personId: String)
    fun deleteFace(personId: String, faceId: String)
    fun clearFace(personId: String)
}

interface FaceStoreChangeListener<in P : Meta, in F : Meta> {
    fun onPersonUpdate(person: P)
    fun onFaceUpdate(personId: String, face: F)
    fun onFaceDataDelete(personId: String)
    fun onFaceDelete(personId: String, faceId: String)
    fun onPersonFaceClear(personId: String)
}