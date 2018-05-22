package com.github.charleslzq.faceengine.core

import android.graphics.Rect
import com.github.charleslzq.faceengine.support.callNullableOnIo
import com.github.charleslzq.faceengine.support.callOnCompute
import com.github.charleslzq.facestore.Meta
import com.github.charleslzq.facestore.ReadOnlyFaceStore

/**
 * Created by charleslzq on 18-3-1.
 */
data class TrackedFace(val rect: Rect, val degree: Int)

interface FaceTracker<in I> {
    fun trackFace(image: I): List<TrackedFace>
}

interface GenderDetector<in I, out G> {
    fun detectGender(image: I): List<G>
}

interface AgeDetector<in I, out A> {
    fun detectAge(image: I): List<A>
}

interface FaceEngine<in I, P : Meta, F : Meta> {
    fun detect(image: I): Map<TrackedFace, F> = emptyMap()
    fun search(face: F): P?
}

interface FaceOfflineEngine<in I, P : Meta, F : Meta, R : Comparable<R>, out S : ReadOnlyFaceStore<P, F>> : FaceEngine<I, P, F> {
    val store: S
    var threshold: R
    fun calculateSimilarity(savedFace: F, newFace: F): R
    fun search(newFace: F, store: ReadOnlyFaceStore<P, F>) = store.getPersonIds()
            .mapNotNull { store.getPerson(it) }
            .filter { store.getFaceIdList(it.id).isNotEmpty() }
            .mapNotNull {
                store.getFaceIdList(it.id).mapNotNull { faceId ->
                    store.getFace(it.id, faceId)?.let { calculateSimilarity(it, newFace) }
                }.max()?.run { it to this }
            }.maxBy { it.second }

    fun searchForScore(face: F) = search(face, store)
    override fun search(face: F) = searchForScore(face)?.takeIf { it.second >= threshold }?.first
}

open class FaceOfflineEngineRxDelegate<in I, P : Meta, F : Meta, R : Comparable<R>, out S : ReadOnlyFaceStore<P, F>>(
        protected val delegate: FaceOfflineEngine<I, P, F, R, S>
) : FaceOfflineEngine<I, P, F, R, S> {
    final override var threshold: R
        get() = delegate.threshold
        set(value) {
            delegate.threshold = value
        }
    final override val store: S
        get() = delegate.store

    final override fun detect(image: I) = callOnCompute { delegate.detect(image) }

    final override fun calculateSimilarity(savedFace: F, newFace: F) =
            callOnCompute { delegate.calculateSimilarity(savedFace, newFace) }

    final override fun search(newFace: F, store: ReadOnlyFaceStore<P, F>) =
            callNullableOnIo { delegate.search(newFace, store) }

    final override fun searchForScore(face: F) = callNullableOnIo { delegate.searchForScore(face) }

    final override fun search(face: F) =
            callNullableOnIo { delegate.search(face) }
}