package com.github.charleslzq.faceengine.view

import com.github.charleslzq.faceengine.view.config.CameraCapabilities
import com.github.charleslzq.faceengine.view.config.CameraParameters
import io.fotoapparat.parameter.Resolution
import io.reactivex.Single

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
interface Selector<T> {
    fun select(choices: Iterable<T>): T?
}

interface CameraPreviewOperator {
    val id: String
    val source: CameraOperatorSource
    fun getCapabilities(): Single<CameraCapabilities>
    fun getCurrentParameters(): Single<CameraParameters>
    fun startPreview(requestParameters: CameraParameters)
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