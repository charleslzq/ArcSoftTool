package com.github.charleslzq.faceengine.view.task

import com.github.charleslzq.faceengine.view.SourceAwarePreviewFrame
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class CoroutineFrameTaskRunner(
        override var sampleInterval: Long
) : FrameTaskRunner {
    private val channel = Channel<Pair<Long, SourceAwarePreviewFrame>>()
    private val lastSubmit = AtomicLong(0)

    override fun <T> transformAndSubmit(raw: T, transform: (T) -> SourceAwarePreviewFrame?) {
        launch {
            transform(raw)?.let {
                val current = System.currentTimeMillis()
                if (current - lastSubmit.get() >= sampleInterval) {
                    channel.send(Pair(current, it))
                    lastSubmit.set(current)
                }
            }
        }
    }

    override fun subscribe(timeout: Long, timeUnit: TimeUnit, processor: (SourceAwarePreviewFrame) -> Unit) {
        launch {
            channel.consumeEach {
                if (it.first >= lastSubmit.get()) {
                    launch {
                        withTimeout(timeout, timeUnit) {
                            processor(it.second)
                        }
                    }
                }
            }
        }
    }

    override fun close() {
        channel.close()
    }
}