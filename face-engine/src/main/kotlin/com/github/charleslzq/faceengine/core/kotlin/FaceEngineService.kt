package com.github.charleslzq.faceengine.core.kotlin

import android.app.Service
import android.content.Intent
import android.os.Binder
import com.github.charleslzq.faceengine.core.kotlin.store.Meta
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore

/**
 * Created by charleslzq on 18-3-6.
 */
class FaceEngineService<P : Meta, F : Meta, R : Comparable<R>>(
    private val engine: FaceEngine<P, F, R>
) : Binder(), FaceDetectionEngine<F> by engine, FaceRecognitionEngine<P, F, R> by engine {
    val store: ReadWriteFaceStore<P, F>
        get() = engine.store

    fun search(face: F) = search(face, store)
}

abstract class FaceEngineServiceBackground<P : Meta, F : Meta, R : Comparable<R>> : Service() {
    protected val engine by lazy {
        createEngine()
    }

    override fun onBind(p0: Intent?) = FaceEngineService(engine)
    override fun onDestroy() {
        (engine as? AutoCloseable)?.close()
        super.onDestroy()
    }

    abstract fun createEngine(): FaceEngine<P, F, R>
}