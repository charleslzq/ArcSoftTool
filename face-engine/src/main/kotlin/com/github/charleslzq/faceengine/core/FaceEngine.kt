package com.github.charleslzq.faceengine.core

import com.github.charleslzq.faceengine.support.callNullableOnIo
import com.github.charleslzq.faceengine.support.callOnCompute
import com.github.charleslzq.facestore.Meta
import com.github.charleslzq.facestore.ReadOnlyFaceStore

/**
 * Created by charleslzq on 18-3-1.
 */
interface FaceEngine<in I, P : Meta, F : Meta, R : Comparable<R>, out S : ReadOnlyFaceStore<P, F>> {
    val store: S
    fun detect(image: I): List<F> = emptyList()
    fun calculateSimilarity(savedFace: F, newFace: F): R
    fun search(newFace: F, store: ReadOnlyFaceStore<P, F>) = store.getPersonIds()
            .mapNotNull { store.getPerson(it) }
            .filter { store.getFaceIdList(it.id).isNotEmpty() }
            .mapNotNull {
                store.getFaceIdList(it.id).mapNotNull { faceId ->
                    store.getFace(it.id, faceId)?.let { calculateSimilarity(it, newFace) }
                }.max()?.run { it to this }
            }.maxBy { it.second }

    fun search(face: F) = search(face, store)
}

open class FaceEngineRxDelegate<in I, P : Meta, F : Meta, R : Comparable<R>, out S : ReadOnlyFaceStore<P, F>>(
        protected val delegate: FaceEngine<I, P, F, R, S>
) : FaceEngine<I, P, F, R, S> {
    final override val store: S
        get() = delegate.store

    final override fun detect(image: I) = callOnCompute { delegate.detect(image) }

    final override fun calculateSimilarity(savedFace: F, newFace: F) =
            callOnCompute { delegate.calculateSimilarity(savedFace, newFace) }

    final override fun search(newFace: F, store: ReadOnlyFaceStore<P, F>) =
            callNullableOnIo { delegate.search(newFace, store) }

    final override fun search(face: F) =
            callNullableOnIo { delegate.search(face) }
}