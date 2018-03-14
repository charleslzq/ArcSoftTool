package com.github.charleslzq.faceengine.core

/**
 * Created by charleslzq on 18-3-14.
 */
interface AgeDetector<in I, out A> {
    fun detectAge(image: I): List<A>
}