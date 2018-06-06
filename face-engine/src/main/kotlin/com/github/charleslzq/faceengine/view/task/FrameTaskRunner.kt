package com.github.charleslzq.faceengine.view.task

import com.github.charleslzq.faceengine.view.SourceAwarePreviewFrame
import java.util.concurrent.TimeUnit

interface FrameTaskRunner : AutoCloseable {
    var enableSample: Boolean
    var sampleInterval: Long
    fun <T> transformAndSubmit(raw: T, transform: (T) -> SourceAwarePreviewFrame?)
    fun submit(previewFrame: SourceAwarePreviewFrame) = transformAndSubmit(previewFrame) { it }
    fun subscribe(
            timeout: Long = 2000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            processor: (SourceAwarePreviewFrame) -> Unit
    )
}