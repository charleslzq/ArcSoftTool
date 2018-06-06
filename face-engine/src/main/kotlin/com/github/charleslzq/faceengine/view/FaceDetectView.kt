package com.github.charleslzq.faceengine.view

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.github.charleslzq.faceengine.core.R
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.core.Updater
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.view.CameraView
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
    private var cameraPreviewConfiguration: CameraPreviewConfiguration = attributeSet?.let { context.obtainStyledAttributes(it, R.styleable.FaceDetectView) }?.extract {
        CameraPreviewConfiguration.from(this)
    } ?: CameraPreviewConfiguration()
    private val cameraView = CameraView(context, attributeSet, defStyle).also {
        it.setScaleType(ScaleType.CenterInside)
        addView(it)
    }
    private val trackView = TrackView(context, attributeSet, defStyle).also {
        it.applyConfiguration(cameraPreviewConfiguration)
        addView(it)
    }
    private val cameraSources = listOf(
            UVCCameraOperatorSource(
                    context,
                    cameraView,
                    cameraPreviewConfiguration,
                    this::onNewDevice,
                    this::onDisconnect),
            FotoCameraOperatorSource(
                    context,
                    cameraView,
                    cameraPreviewConfiguration)
    )
    override val cameras: List<CameraPreviewOperator>
        get() = cameraSources.flatMap { it.cameras }
    var selectCamera: (Iterable<CameraPreviewOperator>) -> CameraPreviewOperator? = { null }
        set(value) {
            val oldCamera = field(cameras)
            val newCamera = value(cameras)
            if (oldCamera != newCamera) {
                oldCamera?.stopPreview()
                field = value
                newCamera?.onSelected()
                newCamera?.startPreview()
            }
        }
    val selectedCamera: CameraPreviewOperator?
        get() = selectCamera(cameras)
    val selectedSource: CameraOperatorSource?
        get() = selectedCamera?.source


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        cameraView.layout(left, top, right, bottom)
        trackView.layout(
                cameraView.textureView.left,
                cameraView.textureView.top,
                cameraView.textureView.right,
                cameraView.textureView.bottom
        )
    }

    @JvmOverloads
    fun onPreview(
            timeout: Long = 2000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            processor: (SourceAwarePreviewFrame) -> Unit
    ) = cameraPreviewConfiguration.frameTaskRunner.subscribe(timeout, timeUnit, processor)

    @JvmOverloads
    fun onPreview(
            timeout: Long = 2000,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
            frameConsumer: FrameConsumer
    ) = cameraPreviewConfiguration.frameTaskRunner.subscribe(timeout, timeUnit, {
        frameConsumer.accept(it)
    })

    fun updateTrackFaces(faces: Collection<TrackedFace>) {
        if (cameraPreviewConfiguration.showRect) {
            trackView.resetRects(faces)
        }
    }

    fun selectNext() {
        selectedCamera?.let {
            selectAt(cameras.indexOf(it) + 1)
        }
    }

    fun selectAt(index: Int) {
        selectCamera = {
            it.elementAtOrNull(index % it.count())
        }
    }

    fun selectById(id: String) {
        selectCamera = {
            it.firstOrNull { it.id == id }
        }
    }

    fun selectCamera(selector: Selector<CameraPreviewOperator>) {
        selectCamera = { selector.select(it) }
    }

    private fun onNewDevice(camera: CameraPreviewOperator) {
        if (cameraPreviewConfiguration.autoSwitchToNewDevice) {
            selectById(camera.id)
        }
    }

    private fun onDisconnect(camera: CameraPreviewOperator) {
        if (selectedCamera != null && selectedCamera!!.id == camera.id) {
            selectCamera = { it.firstOrNull() }
        }
    }

    override fun open() {
        cameraSources.forEach { it.open() }
        if (selectedCamera == null || !selectedCamera!!.isPreviewing()) {
            selectCamera = { it.firstOrNull() }
        }
    }

    fun pause() {
        selectedCamera?.stopPreview()
    }

    override fun close() {
        cameraSources.forEach {
            it.close()
        }
        cameraPreviewConfiguration.frameTaskRunner.close()
    }

    fun updateConfiguration(update: (CameraPreviewConfiguration) -> CameraPreviewConfiguration?) {
        update(cameraPreviewConfiguration)?.let { newConfig ->
            cameraPreviewConfiguration = newConfig
            cameraSources.forEach { it.applyConfiguration(cameraPreviewConfiguration) }
            trackView.applyConfiguration(cameraPreviewConfiguration)
        }
    }

    fun updateConfiguration(updater: Updater<CameraPreviewConfiguration>) = updateConfiguration { updater.update(it) }
}