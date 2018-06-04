package com.github.charleslzq.faceengine.support

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder

interface ServiceInvoker<in S> {
    fun invoke(service: S)
}

interface ServiceCaller<in S, out T> {
    fun call(service: S): T?
}

interface ServiceConnectionBuilder<S> {
    fun getBuilder() = ServiceConnectionProvider.Builder<S>()
}

class ServiceConnectionProvider<S>
private constructor(
        private val afterConnected: (S) -> Unit = {},
        private val beforeDisconnect: (S) -> Unit = {}
) : ServiceConnection {
    private var binder: ServiceBinder<S>? = null
    val isConnected: Boolean
        get() = binder != null

    fun <T> whenConnected(handler: (S) -> T?) = binder?.instance?.let(handler)

    fun whenConnected(invoker: ServiceInvoker<S>) = binder?.instance?.let { invoker.invoke(it) }

    fun <T> whenConnected(caller: ServiceCaller<S, T>) = binder?.instance?.let { caller.call(it) }

    inline fun <reified B : ServiceBackground<S>> bind(context: Context, flags: Int = BIND_AUTO_CREATE) = context.bindService(Intent(context, B::class.java), this, flags)

    @JvmOverloads
    fun <B : ServiceBackground<S>> bind(context: Context, clazz: Class<B>, flags: Int = BIND_AUTO_CREATE) = context.bindService(Intent(context, clazz), this, flags)

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        @Suppress("UNCHECKED_CAST")
        binder = service as? ServiceBinder<S>
        binder?.instance?.let(afterConnected)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        binder?.instance?.let(beforeDisconnect)
        binder = null
    }

    class Builder<S> {
        private var afterConnected: (S) -> Unit = {}
        private var beforeDisconnect: (S) -> Unit = {}

        fun beforeDisconnect(handler: (S) -> Unit) = also {
            it.beforeDisconnect = handler
        }

        fun beforeDisconnect(invoker: ServiceInvoker<S>) = also {
            it.beforeDisconnect = { invoker.invoke(it) }
        }

        fun afterConnected(handler: (S) -> Unit) = also {
            it.afterConnected = handler
        }

        fun afterConnected(invoker: ServiceInvoker<S>) = also {
            it.afterConnected = { invoker.invoke(it) }
        }

        fun build() = ServiceConnectionProvider(
                afterConnected = afterConnected,
                beforeDisconnect = beforeDisconnect
        )
    }
}

class ServiceBinder<S>(
        val instance: S
) : Binder()

abstract class ServiceBackground<S> : Service() {
    protected val binder by lazy {
        ServiceBinder(createService())
    }

    override fun onBind(intent: Intent?) = binder

    override fun onDestroy() {
        (binder.instance as? AutoCloseable)?.close()
        super.onDestroy()
    }

    abstract fun createService(): S
}