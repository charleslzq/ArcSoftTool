package com.github.charleslzq.faceengine.view

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.faceEngineTaskExecutor
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.view.CameraView
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Created by charleslzq on 18-3-8.
 */
val CameraView.textureView
    get() = getChildAt(0) as TextureView

class FaceDetectView
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0) :
        FrameLayout(context, attributeSet, defStyle), CameraSource {
    private var cameraPreviewConfiguration: CameraPreviewConfiguration = CameraPreviewConfiguration.from(context, attributeSet, defStyle)
    private val cameraView = CameraView(context, attributeSet, defStyle).also {
        it.setScaleType(ScaleType.CenterInside)
        addView(it)
    }
    private val trackView = TrackView(context, attributeSet, defStyle).also {
        it.applyConfiguration(cameraPreviewConfiguration)
        addView(it)
    }
    private val cameraSources = listOf(
            UVCCameraOperatorSource(context, cameraView, cameraPreviewConfiguration, this::switchTo),
            FotoCameraOperatorSource(context, cameraView, cameraPreviewConfiguration, this::switchTo)
    )

    var operatorSourceSelector: (Iterable<CameraOperatorSource>) -> CameraOperatorSource? = { null }
        set(value) {
            val oldSelection = field(cameraSources)
            val newSelection = value(cameraSources)
            if (oldSelection != newSelection) {
                oldSelection?.getCameras()?.forEach { it.stopPreview() }
                oldSelection?.selected = false
                field = value
                newSelection?.selected = true
                newSelection?.selectedCamera?.startPreview()
            }
        }
    val selectedSource: CameraOperatorSource?
        get() = operatorSourceSelector(cameraSources)
    override val selectedCamera: CameraPreviewOperator?
        get() = operatorSourceSelector(cameraSources)?.selectedCamera

    private val disposables = mutableListOf<Disposable>()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        cameraView.layout(left, top, right, bottom)
        trackView.layout(
                cameraView.textureView.left,
                cameraView.textureView.top,
                cameraView.textureView.right,
                cameraView.textureView.bottom
        )
    }

    override fun onPreviewFrame(scheduler: Scheduler, processor: (CameraPreview.PreviewFrame) -> Unit) = CompositeDisposable(
            cameraSources.map {
                it.onPreviewFrame(scheduler, processor)
            }
    ).also { disposables.add(it) }

    override fun onPreviewFrame(scheduler: Scheduler, frameConsumer: CameraPreview.FrameConsumer) = CompositeDisposable(
            cameraSources.map {
                it.onPreviewFrame(scheduler, frameConsumer)
            }
    ).also { disposables.add(it) }

    @JvmOverloads
    fun onPreview(
            timeout: Long = 2000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            scheduler: Scheduler = Schedulers.computation(),
            processor: (CameraPreview.PreviewFrame) -> Unit
    ) = onPreviewFrame(scheduler) {
        faceEngineTaskExecutor.run {
            executeInTimeout(selectedCamera!!.id, timeout, timeUnit) {
                processor(it)
            }
            logStatus()
        }
    }

    @JvmOverloads
    fun onPreview(
            timeout: Long = 2000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            scheduler: Scheduler = Schedulers.computation(),
            frameConsumer: CameraPreview.FrameConsumer
    ) = onPreviewFrame(scheduler) {
        faceEngineTaskExecutor.run {
            executeInTimeout(selectedCamera!!.id, timeout, timeUnit) {
                frameConsumer.accept(it)
            }
            logStatus()
        }
    }

    override fun getCameras() = cameraSources.flatMap { it.getCameras() }

    fun updateTrackFaces(faces: Collection<TrackedFace>) {
        if (cameraPreviewConfiguration.showRect) {
            trackView.resetRects(faces)
        }
    }

    override fun start() {
        cameraSources.forEach { it.start() }
        if (selectedCamera == null || !selectedCamera!!.isPreviewing()) {
            cameraSources.firstOrNull { it.getCameras().isNotEmpty() }?.let {
                val sourceId = it.id
                operatorSourceSelector = {
                    it.firstOrNull { it.id == sourceId }
                }
            }
        }
    }

    fun selectNext() {
        if (selectedSource != null && selectedCamera != null) {
            faceEngineTaskExecutor.cancelTasks()
            selectedSource!!.getCameras().run {
                val index = indexOf(selectedCamera!!)
                if (index + 1 < size) {
                    selectedSource!!.operatorSelector = {
                        it.elementAt(index + 1)
                    }
                } else {
                    val sourceIndex = cameraSources.indexOf(selectedSource!!)
                    val availableSourceIndex = cameraSources.indices.firstOrNull {
                        it > sourceIndex && cameraSources[it].getCameras().isNotEmpty()
                    }
                    operatorSourceSelector = if (availableSourceIndex != null) {
                        { it.elementAt(availableSourceIndex) }
                    } else {
                        { it.firstOrNull { it.getCameras().isNotEmpty() } }
                    }
                    selectedSource!!.operatorSelector = {
                        it.elementAt(0)
                    }
                }
            }
        }
    }

    fun pause() {
        selectedCamera?.stopPreview()
    }

    fun selectSource(selector: (Iterable<CameraOperatorSource>) -> CameraOperatorSource?) {
        operatorSourceSelector = selector
    }

    fun selectSource(selector: SourceSelector) {
        operatorSourceSelector = { selector.select(it) }
    }

    override fun close() {
        cameraSources.forEach {
            it.close()
        }
        disposables.filter { !it.isDisposed }.forEach { it.dispose() }
        disposables.clear()
        faceEngineTaskExecutor.cancelTasks()
    }

    override fun applyConfiguration(cameraPreviewConfiguration: CameraPreviewConfiguration) {
        updateConfiguration { cameraPreviewConfiguration }
    }

    private fun switchTo(id: String) {
        operatorSourceSelector = {
            it.firstOrNull { it.id == id }
        }
    }

    private fun updateConfiguration(generator: CameraPreviewConfiguration.() -> CameraPreviewConfiguration) {
        cameraPreviewConfiguration = generator(cameraPreviewConfiguration)
        cameraSources.forEach { it.applyConfiguration(cameraPreviewConfiguration) }
        trackView.applyConfiguration(cameraPreviewConfiguration)
    }

    class CompositeDisposable(private val disposables: List<Disposable>) : Disposable {
        override fun isDisposed() = disposables.all { it.isDisposed }

        override fun dispose() = disposables.forEach { it.dispose() }
    }

    @FunctionalInterface
    interface SourceSelector {
        fun select(choices: Iterable<CameraOperatorSource>): CameraOperatorSource?
    }

    companion object {
        const val TAG = "FaceDetectView"
    }
}