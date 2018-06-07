package com.github.charleslzq.faceengine.view.task

import com.github.charleslzq.faceengine.support.runOn
import com.github.charleslzq.faceengine.view.SourceAwarePreviewFrame
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

const val DEFAULT_THREAD_SIZE = 5

fun getThreadPoolService(size: Int = DEFAULT_THREAD_SIZE) = Executors.newFixedThreadPool(size) as ThreadPoolExecutor

class RxTaskExecutor
@JvmOverloads
constructor(
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

class RxFrameTaskRunner
@JvmOverloads
constructor(
        override var enableSample: Boolean,
        override var sampleInterval: Long,
        private val produceScheduler: Scheduler = Schedulers.computation(),
        private val consumeScheduler: Scheduler = Schedulers.computation()
) : FrameTaskRunner {
    private val executor: RxTaskExecutor = RxTaskExecutor()
    private val publisher = PublishSubject.create<Pair<Long, SourceAwarePreviewFrame>>()
    private val lastSubmit = AtomicLong(0)

    override fun <T> transformAndSubmit(raw: T, transform: (T) -> SourceAwarePreviewFrame?) {
        runOn(produceScheduler) {
            transform(raw)?.let {
                val current = System.currentTimeMillis()
                if (!enableSample || current - lastSubmit.get() >= sampleInterval) {
                    publisher.onNext(Pair(current, it))
                    lastSubmit.set(current)
                }
            }
        }
    }

    override fun subscribe(timeout: Long, timeUnit: TimeUnit, processor: (SourceAwarePreviewFrame) -> Unit) {
        publisher.observeOn(consumeScheduler)
                .subscribe {
                    if (!enableSample || it.first >= lastSubmit.get()) {
                        executor.executeInTimeout(timeout, timeUnit) {
                            processor(it.second)
                        }
                    }
                }
    }

    override fun close() {
        executor.cancelTasks()
    }
}