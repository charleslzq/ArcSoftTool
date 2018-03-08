package com.github.charleslzq.faceengine.core.kotlin.store

import com.github.charleslzq.faceengine.core.kotlin.support.callNullableOnIo
import com.github.charleslzq.faceengine.core.kotlin.support.callOnIo
import com.github.charleslzq.faceengine.core.kotlin.support.runOnIo
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
    fun getPersonIds(): List<String> = emptyList()
    fun getFaceData(personId: String): FaceData<P, F>? = null
    fun getPerson(personId: String): P? = null
    fun getFaceIdList(personId: String): List<String> = emptyList()
    fun getFace(personId: String, faceId: String): F? = null
}

open class ReadOnlyFaceStoreRxDelegate<out P : Meta, out F : Meta, out D : ReadOnlyFaceStore<P, F>>(
    protected val delegate: D
) : ReadOnlyFaceStore<P, F> {
    final override fun getPersonIds() = callOnIo { delegate.getPersonIds() }
    final override fun getFaceData(personId: String) =
        callNullableOnIo { delegate.getFaceData(personId) }

    final override fun getPerson(personId: String) =
        callNullableOnIo { delegate.getPerson(personId) }

    final override fun getFaceIdList(personId: String) =
        callOnIo { delegate.getFaceIdList(personId) }

    final override fun getFace(personId: String, faceId: String) =
        callNullableOnIo { delegate.getFace(personId, faceId) }
}

interface ReadWriteFaceStore<P : Meta, F : Meta> : ReadOnlyFaceStore<P, F> {
    fun savePerson(person: P) {}
    fun saveFace(personId: String, face: F) {}
    fun saveFaceData(faceData: FaceData<P, F>) {}
    fun deleteFaceData(personId: String) {}
    fun deleteFace(personId: String, faceId: String) {}
    fun clearFace(personId: String) {}
}

class ReadWriteFaceStoreRxDelegate<P : Meta, F : Meta, out D : ReadWriteFaceStore<P, F>>(
    delegate: D
) : ReadOnlyFaceStoreRxDelegate<P, F, D>(delegate), ReadWriteFaceStore<P, F> {
    override fun savePerson(person: P) {
        runOnIo { delegate.savePerson(person) }
    }

    override fun saveFace(personId: String, face: F) {
        runOnIo { delegate.saveFace(personId, face) }
    }

    override fun saveFaceData(faceData: FaceData<P, F>) {
        runOnIo { delegate.saveFaceData(faceData) }
    }

    override fun deleteFaceData(personId: String) {
        runOnIo { delegate.deleteFaceData(personId) }
    }

    override fun deleteFace(personId: String, faceId: String) {
        runOnIo { delegate.deleteFace(personId, faceId) }
    }

    override fun clearFace(personId: String) {
        runOnIo { delegate.clearFace(personId) }
    }
}

interface FaceStoreChangeListener<in P : Meta, in F : Meta> {
    fun onPersonUpdate(person: P) {}
    fun onFaceUpdate(personId: String, face: F) {}
    fun onFaceDataDelete(personId: String) {}
    fun onFaceDelete(personId: String, faceId: String) {}
    fun onPersonFaceClear(personId: String) {}
}

class FaceStoreChangeListenerRxDelegate<in P : Meta, in F : Meta>(
    private val delegate: FaceStoreChangeListener<P, F>
) : FaceStoreChangeListener<P, F> {
    override fun onPersonUpdate(person: P) {
        runOnIo { delegate.onPersonUpdate(person) }
    }

    override fun onFaceUpdate(personId: String, face: F) {
        runOnIo { delegate.onFaceUpdate(personId, face) }
    }

    override fun onFaceDataDelete(personId: String) {
        runOnIo { delegate.onFaceDataDelete(personId) }
    }

    override fun onFaceDelete(personId: String, faceId: String) {
        runOnIo { delegate.onFaceDelete(personId, faceId) }
    }

    override fun onPersonFaceClear(personId: String) {
        runOnIo { delegate.onPersonFaceClear(personId) }
    }
}