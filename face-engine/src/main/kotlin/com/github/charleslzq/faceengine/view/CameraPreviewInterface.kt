package com.github.charleslzq.faceengine.view

import com.github.charleslzq.faceengine.view.config.CameraCapabilities
import com.github.charleslzq.faceengine.view.config.CameraParameters
import com.github.charleslzq.faceengine.view.config.CameraPreviewRequest
import io.fotoapparat.parameter.Resolution

sealed class PreviewFrame(
        val size: Resolution,
        val image: ByteArray,
        val rotation: Int
)

class SimplePreviewFrame(
        size: Resolution,
        image: ByteArray,
        rotation: Int
) : PreviewFrame(size, image, rotation)

class SourceAwarePreviewFrame(
        val source: String,
        val sequence: Int,
        size: Resolution,
        image: ByteArray,
        rotation: Int
) : PreviewFrame(size, image, rotation)

@FunctionalInterface
interface FrameConsumer {
    fun accept(previewFrame: SourceAwarePreviewFrame)
}

@FunctionalInterface
interface Consumer<T> {
    fun consume(data: T)
}

@FunctionalInterface
interface Selector<T> {
    fun select(choices: Iterable<T>): T?
}

interface CameraPreviewOperator {
    val id: String
    val source: CameraOperatorSource
    fun getCapabilities(): CameraCapabilities
    fun getCurrentParameters(): CameraParameters
    fun startPreview()
    fun updateConfig(request: CameraPreviewRequest)
    fun stopPreview()
    fun isPreviewing(): Boolean
}

interface CameraSource : AutoCloseable {
    val cameras: List<CameraPreviewOperator>
    fun open() {}
    override fun close() {}
}

abstract class CameraOperatorSource : CameraSource {
    abstract val id: String
}