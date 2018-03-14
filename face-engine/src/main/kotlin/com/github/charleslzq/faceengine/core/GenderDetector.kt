package com.github.charleslzq.faceengine.core

/**
 * Created by charleslzq on 18-3-14.
 */
interface GenderDetector<in I, out G> {
    fun detectGender(image: I): List<G>
}