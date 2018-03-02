package com.github.charleslzq.faceengine.core.kotlin

import android.graphics.Bitmap
import com.github.charleslzq.faceengine.core.kotlin.store.Meta
import com.github.charleslzq.faceengine.core.kotlin.store.ReadOnlyFaceStore
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore
import com.github.charleslzq.faceengine.core.kotlin.support.TypeHolder

/**
 * Created by charleslzq on 18-3-1.
 */
interface FaceRecognitionEngine<P : Meta, in F : Meta, R : Comparable<R>> {
    fun calculateSimilarity(savedFace: F, newFace: F): R
    fun search(newFace: F, store: ReadOnlyFaceStore<P, F>) = store.getPersonIds()
        .mapNotNull { store.getPerson(it) }
        .filter { store.getFaceIdList(it.id).isNotEmpty() }
        .maxBy {
            store.getFaceIdList(it.id).mapNotNull { faceId ->
                store.getFace(it.id, faceId)?.let { calculateSimilarity(it, newFace) }
            }.max()!!
        }
}

interface FaceDetectionEngine<out F : Meta> {
    fun detect(image: Bitmap): List<F>
}

interface FaceEngineBinder<P : Meta, F : Meta, R : Comparable<R>> {
    fun createStore(): ReadWriteFaceStore<P, F>
    fun createFaceRecognitionEngine(): FaceRecognitionEngine<P, F, R>
    fun createFaceDetectionEngine(): FaceDetectionEngine<F>
}

abstract class FaceEngine<S, P : Meta, F : Meta, R : Comparable<R>>(
    faceEngineBinder: FaceEngineBinder<P, F, R>
) : TypeHolder<S>,
    FaceDetectionEngine<F> by faceEngineBinder.createFaceDetectionEngine(),
    FaceRecognitionEngine<P, F, R> by faceEngineBinder.createFaceRecognitionEngine()
        where S : TypeHolder<S> {
    val store = faceEngineBinder.createStore()
}