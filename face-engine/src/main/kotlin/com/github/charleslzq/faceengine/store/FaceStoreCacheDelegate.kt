package com.github.charleslzq.faceengine.store

import com.github.charleslzq.facestore.*
import com.github.charleslzq.pacsdemo.support.MemCache

/**
 * Created by charleslzq on 18-3-13.
 */
open class ReadOnlyFaceStoreCacheDelegate<P : Meta, F : Meta, out D : ReadOnlyFaceStore<P, F>>(
        protected val delegate: D,
        personCacheSize: Int = 100,
        faceCacheSize: Int = 20
) : ReadOnlyFaceStore<P, F> {
    final override val dataType: FaceDataType<P, F>
        get() = delegate.dataType
    protected val personCache = MemCache(delegate.dataType.personClass, personCacheSize)
    protected val faceCache = MemCache(delegate.dataType.faceClass, faceCacheSize)
    protected val personFaceMapper by lazy {
        delegate.getPersonIds().map {
            it to delegate.getFaceIdList(it)
        }.toMap().toMutableMap()
    }

    final override fun getPersonIds() = personFaceMapper.keys.toList()
    final override fun getFaceData(personId: String) = getPerson(personId)?.run {
        FaceData(this, getFaceIdList(personId).mapNotNull { getFace(personId, it) })
    }

    final override fun getPerson(personId: String) =
            personCache.load(personId) { delegate.getPerson(personId) }

    final override fun getFaceIdList(personId: String) = personFaceMapper[personId] ?: emptyList()

    final override fun getFace(personId: String, faceId: String) =
            faceCache.load(personId, faceId) { delegate.getFace(personId, faceId) }
}

class ReadWriteFaceStoreCacheDelegate<P : Meta, F : Meta, out D : ReadWriteFaceStore<P, F>>(
        delegate: D,
        personCacheSize: Int = 100,
        faceCacheSize: Int = 20
) : ReadOnlyFaceStoreCacheDelegate<P, F, D>(delegate, personCacheSize, faceCacheSize), ReadWriteFaceStore<P, F> {
    override fun savePerson(person: P) {
        delegate.savePerson(person)
        if (personCache.contains(person.id)) {
            personCache.update(
                    args = person.id,
                    data = person
            )
        }
        fillMap(person.id)
    }

    override fun saveFace(personId: String, face: F) {
        delegate.saveFace(personId, face)
        if (faceCache.contains(personId, face.id)) {
            faceCache.update(
                    personId, face.id,
                    data = face
            )
        }
        fillMap(personId)
        personFaceMapper[personId] = personFaceMapper[personId]!!.toMutableList().apply {
            add(face.id)
        }.toList()
    }

    override fun saveFaceData(faceData: FaceData<P, F>) {
        savePerson(faceData.person)
        faceData.faces.forEach { saveFace(faceData.person.id, it) }
    }

    override fun deleteFaceData(personId: String) {
        delegate.deleteFaceData(personId)
        personFaceMapper.remove(personId)
    }

    override fun deleteFace(personId: String, faceId: String) {
        delegate.deleteFace(personId, faceId)
        if (personFaceMapper.containsKey(personId) && personFaceMapper[personId]!!.contains(faceId)) {
            personFaceMapper[personId] = personFaceMapper[personId]!!.toMutableList().apply {
                remove(faceId)
            }.toList()
        }
    }

    override fun clearFace(personId: String) {
        delegate.clearFace(personId)
        personFaceMapper[personId] = emptyList()
    }

    private fun fillMap(personId: String) {
        if (!personFaceMapper.containsKey(personId)) {
            personFaceMapper[personId] = emptyList()
        }
    }
}