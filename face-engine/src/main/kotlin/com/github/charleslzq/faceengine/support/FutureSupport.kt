package com.github.charleslzq.faceengine.support

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

const val DEFAULT_THREAD_SIZE = 5

fun getThreadPoolService(size: Int = DEFAULT_THREAD_SIZE) = Executors.newFixedThreadPool(size) as ThreadPoolExecutor

class FaceEngineTaskExecutor(
        private val executor: ThreadPoolExecutor = getThreadPoolService()
) {
    private val taskList = mutableListOf<Future<*>>()

    fun logStatus() {
        Log.d(TAG, executor.toString())
    }

    fun <V> submit(callable: () -> V) = executor.submit(callable)

    @JvmOverloads
    fun <V> executeInTimeout(timeout: Long = 500, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, callable: () -> V?): V? = submit(callable).run {
        taskList.add(this)
        try {
            get(timeout, timeUnit)
        } catch (exception: Exception) {
            cancel(true)
            null
        }
    }

    fun cancelTasks() {
        taskList.filter { it.isPending }.forEach { it.cancel(true) }
        taskList.clear()
    }

    private val Future<*>.isPending get() = !isCancelled && !isDone

    companion object {
        const val TAG = "FaceEngineTaskExecutor"
    }
}

val faceEngineTaskExecutor = FaceEngineTaskExecutor()