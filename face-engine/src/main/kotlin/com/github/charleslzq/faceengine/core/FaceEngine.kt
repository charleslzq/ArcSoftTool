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

interface Updater<T> {
    fun update(origin: T): T?
}

interface FaceTracker<in I> {
    fun trackFace(image: I): List<TrackedFace>
}

interface GenderDetector<in I, out G> {
    fun detectGender(image: I): List<G>
}

interface AgeDetector<in I, out A> {
    fun detectAge(image: I): List<A>
}

interface FaceEngine<in I, P, F> {
    fun detect(image: I): Map<TrackedFace, F> = emptyMap()
    fun search(image: I): P?
}

interface FaceEngineWithOptions<in I, P, F, DO, SO> : FaceEngine<I, P, F> {
    var defaultDetectOption: DO
    var defaultSearchOption: SO
    fun searchWithOption(image: I, option: SO): P?
    fun detectWithOption(image: I, option: DO): Map<TrackedFace, F> = emptyMap()
    override fun search(image: I) = searchWithOption(image, defaultSearchOption)
    override fun detect(image: I) = detectWithOption(image, defaultDetectOption)
}

interface FaceOfflineEngine<in I, P : Meta, F : Meta, R : Comparable<R>, out S : ReadOnlyFaceStore<P, F>> : FaceEngine<I, P, F> {
    val store: S
    var threshold: R
    fun calculateSimilarity(savedFace: F, newFace: F): R
    fun searchFaceInStore(newFace: F, store: ReadOnlyFaceStore<P, F>) = store.getPersonIds()
            .mapNotNull { store.getPerson(it) }
            .filter { store.getFaceIdList(it.id).isNotEmpty() }
            .mapNotNull {
                store.getFaceIdList(it.id).mapNotNull { faceId ->
                    store.getFace(it.id, faceId)?.let { calculateSimilarity(it, newFace) }
                }.max()?.run { it to this }
            }.maxBy { it.second }

    fun searchFaceForScore(face: F) = searchFaceInStore(face, store)
    fun searchFace(face: F) = searchFaceForScore(face)?.takeIf { it.second >= threshold }?.first
    override fun search(image: I) = detect(image).values.mapNotNull { searchFaceForScore(it) }.maxBy { it.second }?.takeIf { it.second >= threshold }?.first
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

    final override fun searchFaceInStore(newFace: F, store: ReadOnlyFaceStore<P, F>) =
            callNullableOnIo { delegate.searchFaceInStore(newFace, store) }

    final override fun searchFaceForScore(face: F) = callNullableOnIo { delegate.searchFaceForScore(face) }

    final override fun searchFace(face: F) =
            callNullableOnIo { delegate.searchFace(face) }
}