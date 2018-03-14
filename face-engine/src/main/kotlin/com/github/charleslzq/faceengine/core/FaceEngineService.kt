package com.github.charleslzq.faceengine.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import com.github.charleslzq.facestore.Meta
import com.github.charleslzq.facestore.ReadWriteFaceStore

/**
 * Created by charleslzq on 18-3-6.
 */
open class FaceEngineService<in I, P : Meta, F : Meta, R : Comparable<R>, out S : ReadWriteFaceStore<P, F>>(
        val engine: FaceEngine<I, P, F, R, S>
) : Binder(), FaceEngine<I, P, F, R, S> by engine

abstract class FaceEngineServiceBackground<I, P : Meta, F : Meta, R : Comparable<R>, out S : ReadWriteFaceStore<P, F>> :
        Service() {
    private val engineService by lazy {
        createEngineService()
    }

    override fun onBind(p0: Intent?) = engineService

    override fun onDestroy() {
        (engineService.engine as? AutoCloseable)?.close()
        super.onDestroy()
    }

    abstract fun createEngineService(): FaceEngineService<I, P, F, R, S>
}