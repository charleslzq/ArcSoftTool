package com.github.charleslzq.arcsofttools.kotlin

import android.graphics.Bitmap
import com.github.charleslzq.arcsofttools.kotlin.store.Meta
import com.github.charleslzq.arcsofttools.kotlin.store.ReadOnlyFaceStore

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