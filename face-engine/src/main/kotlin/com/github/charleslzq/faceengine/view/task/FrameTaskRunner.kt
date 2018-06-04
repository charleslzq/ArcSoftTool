package com.github.charleslzq.faceengine.view.task

import com.github.charleslzq.faceengine.view.SourceAwarePreviewFrame
import java.util.concurrent.TimeUnit

interface FrameTaskRunner {
    fun consume(previewFrame: SourceAwarePreviewFrame)
    fun onPreviewFrame(
            timeout: Long = 2000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            processor: (SourceAwarePreviewFrame) -> Unit
    ): Task

    fun compute(runnable: () -> Unit)
    fun cancelAll()

    abstract class Task(
            val processor: (SourceAwarePreviewFrame) -> Unit
    ) {
        abstract fun cancel()
    }
}