@file:JvmName("FutureSupport")

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
    private val taskList = mutableListOf<Pair<String, Future<*>>>()

    fun logStatus() {
        Log.d(TAG, executor.toString())
    }

    fun <V> submit(callable: () -> V) = executor.submit(callable)

    @JvmOverloads
    fun <V> executeInTimeout(cameraId: String, timeout: Long = 500, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, callable: () -> V?): V? {
        taskList.removeAll { !it.second.isPending }
        return if (taskList.size < DEFAULT_THREAD_SIZE) {
            submit(callable).run {
                taskList.add(Pair(cameraId, this))
                try {
                    get(timeout, timeUnit)
                } catch (exception: Exception) {
                    cancel(true)
                    null
                }
            }
        } else {
            null
        }
    }

    fun cancelTasks(cameraId: String? = null) {
        taskList.filter {
            (cameraId?.run { this == it.first } ?: true) && it.second.isPending
        }.forEach { it.second.cancel(true) }
        taskList.clear()
    }

    private val Future<*>.isPending get() = !isCancelled && !isDone

    companion object {
        const val TAG = "FaceEngineTaskExecutor"
    }
}

val faceEngineTaskExecutor = FaceEngineTaskExecutor()