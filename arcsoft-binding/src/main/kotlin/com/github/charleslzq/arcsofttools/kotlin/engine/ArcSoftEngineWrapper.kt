package com.github.charleslzq.arcsofttools.kotlin.engine

/**
 * Created by charleslzq on 18-3-2.
 */
abstract class ArcSoftEngineWrapper<E, out V> : AutoCloseable {
    protected val TAG = this::class.java.simpleName

    private val engine by lazy {
        init()?.let {
            determineVersion(it)?.run {
                it to this
            }
        }
    }

    protected abstract fun init(): E?

    protected abstract fun determineVersion(internalEngine: E): V?

    fun initialized() = engine != null

    fun getEngine() = engine?.first

    fun getVersion() = engine?.second
}