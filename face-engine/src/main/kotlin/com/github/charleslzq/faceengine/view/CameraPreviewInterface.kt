package com.github.charleslzq.faceengine.view

import android.content.res.TypedArray
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
interface Selector<T> {
    fun select(choices: Iterable<T>): T?
}

interface CameraPreviewConfigurable {
    fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration)
}

interface CameraPreviewOperator : CameraPreviewConfigurable {
    val id: String
    val source: CameraOperatorSource
    fun startPreview()
    fun stopPreview()
    fun isPreviewing(): Boolean
    fun onSelected() {}
    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {}
}

interface CameraSource : AutoCloseable {
    val cameras: List<CameraPreviewOperator>
    fun open() {}
    override fun close() {}
}

abstract class CameraOperatorSource : CameraSource, CameraPreviewConfigurable {
    abstract var cameraPreviewConfiguration: CameraPreviewConfiguration
    abstract val id: String

    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {
        this.cameraPreviewConfiguration = cameraPreviewConfiguration
    }
}

fun <R> TypedArray.extract(setup: TypedArray.() -> R): R {
    val result = setup(this)
    recycle()
    return result
}