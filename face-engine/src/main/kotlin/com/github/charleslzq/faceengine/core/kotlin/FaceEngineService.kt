package com.github.charleslzq.faceengine.core.kotlin

import android.app.Service
import android.content.Intent
import android.os.Binder
import com.github.charleslzq.faceengine.core.kotlin.store.Meta
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore

/**
 * Created by charleslzq on 18-3-6.
 */
class FaceEngineServiceImpl<P : Meta, F : Meta, R : Comparable<R>>(
    private val engine: FaceEngine<P, F, R>
) : Binder(), FaceDetectionEngine<F> by engine, FaceRecognitionEngine<P, F, R> by engine {
    val store: ReadWriteFaceStore<P, F>
        get() = engine.store

    fun search(face: F) = search(face, store)
}

open class FaceEngineServiceBackground<P : Meta, F : Meta, R : Comparable<R>>(
    private val engine: FaceEngine<P, F, R>
) : Service() {
    override fun onBind(p0: Intent?) = FaceEngineServiceImpl(engine)
}