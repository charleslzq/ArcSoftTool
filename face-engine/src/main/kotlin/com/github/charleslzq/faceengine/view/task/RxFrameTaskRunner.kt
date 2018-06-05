package com.github.charleslzq.faceengine.view.task

import com.github.charleslzq.faceengine.support.runOnCompute
import com.github.charleslzq.faceengine.view.SourceAwarePreviewFrame
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

const val DEFAULT_THREAD_SIZE = 5

fun getThreadPoolService(size: Int = DEFAULT_THREAD_SIZE) = Executors.newFixedThreadPool(size) as ThreadPoolExecutor

class RxTaskExecutor(
        private val executor: ThreadPoolExecutor = getThreadPoolService()
) {
    private val tasks = mutableListOf<Future<*>>()

    fun <V> submit(callable: () -> V) = executor.submit(callable)

    @JvmOverloads
    fun <V> executeInTimeout(timeout: Long = 500, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, callable: () -> V?): V? {
        tasks.removeAll { !it.isPending }
        return if (tasks.size < executor.maximumPoolSize) {
            submit(callable).run {
                tasks.add(this)
                try {
                    get(timeout, timeUnit)
                } catch (exception: Throwable) {
                    cancel(true)
                    null
                }
            }
        } else {
            null
        }
    }

    fun cancelTasks() {
        tasks.filter {
            it.isPending
        }.forEach { it.cancel(true) }
        tasks.clear()
    }

    private val Future<*>.isPending get() = !isCancelled && !isDone
}

class RxFrameTaskRunner(
        private var sampleInterval: Long,
        private val executor: RxTaskExecutor = RxTaskExecutor()
) : FrameTaskRunner {
    private val publisher = PublishSubject.create<SourceAwarePreviewFrame>()

    override fun <T> transformAndSubmit(raw: T, transform: (T) -> SourceAwarePreviewFrame?) {
        runOnCompute {
            transform(raw)?.let {
                publisher.onNext(it)
            }
        }
    }

    override fun subscribe(timeout: Long, timeUnit: TimeUnit, processor: (SourceAwarePreviewFrame) -> Unit) {
        publisher.observeOn(Schedulers.computation())
                .sample(sampleInterval, TimeUnit.MILLISECONDS)
                .subscribe {
                    executor.executeInTimeout(timeout, timeUnit) {
                        processor(it)
                    }
                }
    }

    override fun close() {
        executor.cancelTasks()
    }
}