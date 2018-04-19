package com.github.charleslzq.faceengine.view

import android.graphics.Bitmap
import io.fotoapparat.parameter.Resolution
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

interface CameraPreview {
    fun onPreviewFrame(
            scheduler: Scheduler = Schedulers.computation(),
            processor: (PreviewFrame) -> Unit
    ): Disposable

    fun onPreviewFrame(
            scheduler: Scheduler = Schedulers.computation(),
            frameConsumer: FrameConsumer
    ): Disposable

    data class PreviewFrame(
            val source: String,
            val size: Resolution,
            val image: ByteArray,
            val rotation: Int,
            val sequence: Int? = null
    )

    @FunctionalInterface
    interface FrameConsumer {
        fun accept(previewFrame: PreviewFrame)
    }
}

interface CameraPreviewOperator {
    val id: String
    fun startPreview()
    fun stopPreview()
    fun isPreviewing(): Boolean
    fun takePicture(): Bitmap?
}

interface CameraSource : CameraPreview, AutoCloseable {
    val selectedCamera: CameraPreviewOperator?
    fun start() {}
    fun getCameras(): List<CameraPreviewOperator>
}

abstract class CameraOperatorSource : CameraSource {
    var operatorSelector: (Iterable<CameraPreviewOperator>) -> CameraPreviewOperator? = { it.firstOrNull() }
        set(value) {
            val oldSelection = field(getCameras())
            val newSelection = value(getCameras())
            if (oldSelection != newSelection) {
                if (selected) {
                    oldSelection?.stopPreview()
                }
                field = value
                if (selected) {
                    onSelected(newSelection)
                    newSelection?.startPreview()
                }
            }
        }
    override val selectedCamera: CameraPreviewOperator?
        get() = operatorSelector(getCameras())
    abstract val sampleInterval: Long
    abstract var selected: Boolean
    abstract val id: String
    abstract val switchToThis: (String) -> Unit

    abstract fun onSelected(operator: CameraPreviewOperator?)
}