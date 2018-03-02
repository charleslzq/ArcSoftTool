package com.github.charleslzq.faceengine.core.kotlin.support

/**
 * Created by charleslzq on 18-3-2.
 */
interface TypeHolder<S> where S : TypeHolder<S> {
    @Suppress("UNCHECKED_CAST")
    fun getType() = this as S
}