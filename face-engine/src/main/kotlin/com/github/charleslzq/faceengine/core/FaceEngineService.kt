package com.github.charleslzq.faceengine.core

import android.app.Service
import android.content.Intent
import android.os.Binder

/**
 * Created by charleslzq on 18-3-6.
 */
open class FaceEngineService<in I, P, F, FE : FaceEngine<I, P, F>>(
        val engine: FE
) : Binder(), FaceEngine<I, P, F> by engine

abstract class FaceEngineServiceBackground<in I, P, F, FE : FaceEngine<I, P, F>> :
        Service() {
    protected val engineService by lazy {
        createEngineService()
    }

    override fun onBind(p0: Intent?) = engineService

    override fun onDestroy() {
        (engineService.engine as? AutoCloseable)?.close()
        super.onDestroy()
    }

    abstract fun createEngineService(): FaceEngineService<I, P, F, FE>
}