package com.github.charleslzq.faceengine.core.kotlin

import android.graphics.Bitmap
import com.github.charleslzq.faceengine.core.kotlin.store.Meta
import com.github.charleslzq.faceengine.core.kotlin.store.ReadOnlyFaceStore
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore

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

class FaceEngine<P : Meta, F : Meta, R : Comparable<R>>(
    val store: ReadWriteFaceStore<P, F>,
    val detectEngine: FaceDetectionEngine<F>,
    val recognitionEngine: FaceRecognitionEngine<P, F, R>
) : FaceDetectionEngine<F> by detectEngine,
    FaceRecognitionEngine<P, F, R> by recognitionEngine