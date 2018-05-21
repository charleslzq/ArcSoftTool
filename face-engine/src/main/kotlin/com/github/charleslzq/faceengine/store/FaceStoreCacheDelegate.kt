package com.github.charleslzq.faceengine.store

import com.github.charleslzq.faceengine.support.MemCache
import com.github.charleslzq.facestore.ListenableReadWriteFaceStore
import com.github.charleslzq.facestore.Meta
import com.github.charleslzq.facestore.ReadOnlyFaceStore

/**
 * Created by charleslzq on 18-3-13.
 */
open class ReadOnlyFaceStoreCacheDelegate<P : Meta, F : Meta, out D : ReadOnlyFaceStore<P, F>>(
        protected val delegate: D,
        personCacheSize: Int = 100,
        faceCacheSize: Int = 20
) : ReadOnlyFaceStore<P, F> {
    final override val faceClass: Class<F>
        get() = delegate.faceClass
    final override val personClass: Class<P>
        get() = delegate.personClass
    protected val personCache = MemCache(personClass, personCacheSize)
    protected val faceCache = MemCache(faceClass, faceCacheSize)
    protected val personFaceMapper by lazy {
        delegate.getPersonIds().map {
            it to delegate.getFaceIdList(it)
        }.toMap().toMutableMap()
    }

    final override fun getPersonIds() = personFaceMapper.keys.toList()

    final override fun getPerson(personId: String) =
            personCache.load(personId) { delegate.getPerson(personId) }

    final override fun getFaceIdList(personId: String) = personFaceMapper[personId] ?: emptyList()

    final override fun getFace(personId: String, faceId: String) =
            faceCache.load(personId, faceId) { delegate.getFace(personId, faceId) }
}

open class ReadWriteFaceStoreCacheDelegate<P : Meta, F : Meta, out D : ListenableReadWriteFaceStore<P, F>>(
        delegate: D,
        personCacheSize: Int = 100,
        faceCacheSize: Int = 20
) : ReadOnlyFaceStoreCacheDelegate<P, F, D>(delegate, personCacheSize, faceCacheSize), ListenableReadWriteFaceStore<P, F> {
    final override val listeners = delegate.listeners

    final override fun savePerson(person: P) {
        delegate.savePerson(person)
        if (personCache.contains(person.id)) {
            personCache.update(
                    person.id,
                    data = person
            )
        }
        fillMap(person.id)
    }

    final override fun saveFace(personId: String, face: F) {
        delegate.saveFace(personId, face)
        if (faceCache.contains(personId, face.id)) {
            faceCache.update(
                    personId, face.id,
                    data = face
            )
        }
        fillMap(personId)
        if (!personFaceMapper[personId]!!.contains(face.id)) {
            personFaceMapper[personId] = personFaceMapper[personId]!!.toMutableList().apply {
                add(face.id)
            }.toList()
        }
    }

    final override fun deletePerson(personId: String) {
        delegate.deletePerson(personId)
        personFaceMapper.remove(personId)
    }

    final override fun deleteFace(personId: String, faceId: String) {
        delegate.deleteFace(personId, faceId)
        if (personFaceMapper.containsKey(personId) && personFaceMapper[personId]!!.contains(faceId)) {
            personFaceMapper[personId] = personFaceMapper[personId]!!.toMutableList().apply {
                remove(faceId)
            }.toList()
        }
    }

    private fun fillMap(personId: String) {
        if (!personFaceMapper.containsKey(personId)) {
            personFaceMapper[personId] = emptyList()
        }
    }
}