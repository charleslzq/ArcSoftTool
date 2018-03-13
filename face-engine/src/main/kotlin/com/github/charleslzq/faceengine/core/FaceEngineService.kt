package com.github.charleslzq.faceengine.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import com.github.charleslzq.facestore.Meta
import com.github.charleslzq.facestore.ReadWriteFaceStore

/**
 * Created by charleslzq on 18-3-6.
 */
open class FaceEngineService<P : Meta, F : Meta, R : Comparable<R>, out S : ReadWriteFaceStore<P, F>>(
        val engine: FaceEngine<P, F, R, S>
) : Binder(), FaceEngine<P, F, R, S> by engine

abstract class FaceEngineServiceBackground<P : Meta, F : Meta, R : Comparable<R>, out S : ReadWriteFaceStore<P, F>> :
        Service() {
    private val engineService by lazy {
        createEngineService()
    }

    override fun onBind(p0: Intent?) = engineService

    override fun onDestroy() {
        (engineService.engine as? AutoCloseable)?.close()
        super.onDestroy()
    }

    abstract fun createEngineService(): FaceEngineService<P, F, R, S>
}