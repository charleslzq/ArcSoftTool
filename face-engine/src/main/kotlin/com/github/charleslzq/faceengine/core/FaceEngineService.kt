package com.github.charleslzq.faceengine.core

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder

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

    class ServiceConnectionWrapper<I, P, F, FE : FaceEngine<I, P, F>, T : FaceEngineService<I, P, F, FE>>(
            private val afterConnected: (T) -> Unit = {},
            private val beforeDisconnect: (T) -> Unit = {}
    ) : ServiceConnection {
        private var instance: T? = null
        val isConnected = instance != null

        fun getEngine() = instance?.engine

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            @Suppress("UNCHECKED_CAST")
            instance = service as? T
            instance?.let(afterConnected)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            instance?.let(beforeDisconnect)
            instance = null
        }

        class Builder<I, P, F, FE : FaceEngine<I, P, F>, T : FaceEngineService<I, P, F, FE>> {
            private var afterConnected: (T) -> Unit = {}
            private var beforeDisconnect: (T) -> Unit = {}

            fun beforeDisconnect(handler: (T) -> Unit) = also {
                it.afterConnected = handler
            }

            fun afterConnected(handler: (T) -> Unit) = also {
                it.beforeDisconnect = handler
            }

            fun build() = ServiceConnectionWrapper(afterConnected, beforeDisconnect)
        }
    }

    interface ConnectionBuilder<I, P, F, FE : FaceEngine<I, P, F>, T : FaceEngineService<I, P, F, FE>> {
        fun getBuilder() = ServiceConnectionWrapper.Builder<I, P, F, FE, T>()
    }
}