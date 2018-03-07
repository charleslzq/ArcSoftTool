package com.github.charleslzq.faceengine.core.kotlin

import android.app.Service
import android.content.Intent
import android.os.Binder
import com.github.charleslzq.faceengine.core.kotlin.store.Meta
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore

/**
 * Created by charleslzq on 18-3-6.
 */
class FaceEngineService<P : Meta, F : Meta, R : Comparable<R>, out S : ReadWriteFaceStore<P, F>>(
    private val engine: FaceEngine<P, F, R, S>
) : Binder(), FaceEngine<P, F, R, S> by engine {
    override val store: S
        get() = engine.store
}

abstract class FaceEngineServiceBackground<P : Meta, F : Meta, R : Comparable<R>, out S : ReadWriteFaceStore<P, F>> :
    Service() {
    private val engine by lazy {
        createEngine()
    }

    override fun onBind(p0: Intent?) = FaceEngineService(engine)

    override fun onDestroy() {
        (engine as? AutoCloseable)?.close()
        super.onDestroy()
    }

    abstract fun createEngine(): FaceEngine<P, F, R, S>
}