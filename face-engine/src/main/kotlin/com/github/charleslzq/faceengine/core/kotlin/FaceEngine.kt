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
interface FaceRecognitionEngine<P : Meta, in F : Meta, R : Comparable<R>> {
    fun calculateSimilarity(savedFace: F, newFace: F): R
    fun search(newFace: F, store: ReadOnlyFaceStore<P, F>) = store.getPersonIds()
        .mapNotNull { store.getPerson(it) }
        .filter { store.getFaceIdList(it.id).isNotEmpty() }
        .mapNotNull {
            store.getFaceIdList(it.id).mapNotNull { faceId ->
                store.getFace(it.id, faceId)?.let { calculateSimilarity(it, newFace) }
            }.max()?.run { it to this }
        }.maxBy { it.second }
}

class FaceRecognitionEngineRxDelegate<P : Meta, in F : Meta, R : Comparable<R>>(
    private val delegate: FaceRecognitionEngine<P, F, R>
) : FaceRecognitionEngine<P, F, R> {
    override fun calculateSimilarity(savedFace: F, newFace: F) =
        callOnCompute { delegate.calculateSimilarity(savedFace, newFace) }

    override fun search(newFace: F, store: ReadOnlyFaceStore<P, F>) =
        callNullableOnCompute { delegate.search(newFace, store) }
}

interface FaceDetectionEngine<out F : Meta> {
    fun detect(image: Bitmap): List<F> = emptyList()
}

class FaceDetectionEngineRxDelegate<out F : Meta>(
    private val delegate: FaceDetectionEngine<F>
) : FaceDetectionEngine<F> {
    override fun detect(image: Bitmap) = callOnCompute { delegate.detect(image) }
}

class FaceEngine<P : Meta, F : Meta, R : Comparable<R>>(
    val store: ReadWriteFaceStore<P, F>,
    val detectEngine: FaceDetectionEngine<F>,
    val recognitionEngine: FaceRecognitionEngine<P, F, R>
) : FaceDetectionEngine<F> by detectEngine,
    FaceRecognitionEngine<P, F, R> by recognitionEngine