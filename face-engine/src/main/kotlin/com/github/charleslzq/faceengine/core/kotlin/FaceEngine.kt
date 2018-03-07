package com.github.charleslzq.faceengine.core.kotlin

import android.graphics.Bitmap
import com.github.charleslzq.faceengine.core.kotlin.store.Meta
import com.github.charleslzq.faceengine.core.kotlin.store.ReadOnlyFaceStore
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore
import com.github.charleslzq.faceengine.core.kotlin.support.callNullableOnCompute
import com.github.charleslzq.faceengine.core.kotlin.support.callOnCompute

/**
 * Created by charleslzq on 18-3-1.
 */
interface FaceEngine<P : Meta, F : Meta, R : Comparable<R>, out S : ReadWriteFaceStore<P, F>> {
    val store: S
    fun detect(image: Bitmap): List<F> = emptyList()
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

class FaceEngineRxDelegate<P : Meta, F : Meta, R : Comparable<R>, out S : ReadWriteFaceStore<P, F>>(
    private val delegate: FaceEngine<P, F, R, S>
) : FaceEngine<P, F, R, S> {
    override val store: S
        get() = delegate.store

    override fun detect(image: Bitmap) = callOnCompute { delegate.detect(image) }
    override fun calculateSimilarity(savedFace: F, newFace: F) =
        callOnCompute { delegate.calculateSimilarity(savedFace, newFace) }

    override fun search(newFace: F, store: ReadOnlyFaceStore<P, F>) =
        callNullableOnCompute { delegate.search(newFace, store) }
}