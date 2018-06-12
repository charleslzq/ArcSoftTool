package com.github.charleslzq.faceengine.view

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.view.config.CameraPreviewConfiguration
import com.github.charleslzq.faceengine.view.config.CameraSettingManager
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.view.CameraView
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
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
    val settingManager = CameraSettingManager()
    val cameraPreviewConfiguration: CameraPreviewConfiguration
        get() = settingManager.loadSetting().cameraPreviewConfiguration
    private val cameraView = CameraView(context, attributeSet, defStyle).also {
        it.setScaleType(ScaleType.CenterInside)
        addView(it)
    }
    private val trackView = TrackView(context, attributeSet, defStyle).also {
        it.getConfiguration = { cameraPreviewConfiguration }
        addView(it)
    }
    private val cameraSources = listOf(
            UVCCameraOperatorSource(
                    context,
                    cameraView,
                    { byteBuffer, transform -> cameraPreviewConfiguration.frameTaskRunner.transformAndSubmit(byteBuffer, transform) },
                    this::onNewDevice,
                    this::onDisconnect),
            FotoCameraOperatorSource(
                    context,
                    cameraView,
                    { frame, transform -> cameraPreviewConfiguration.frameTaskRunner.transformAndSubmit(frame, transform) })
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
                newCamera?.run {
                    startPreview(settingManager.loadParameters(this))
                }
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

    fun selectFirst() {
        cameras.firstOrNull()?.let {
            selectById(it.id)
        }
    }

    fun selectLast() {
        cameras.lastOrNull()?.let {
            selectById(it.id)
        }
    }

    fun selectPrevious() {
        selectedCamera?.let {
            selectAt(cameras.indexOf(it) + cameras.size - 1)
        }
    }

    fun selectNext() {
        selectedCamera?.let {
            selectAt(cameras.indexOf(it) + 1)
        }
    }

    fun selectAt(index: Int) {
        selectById(cameras.elementAt(index % cameras.size).id)
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
            selectCamera = { it.firstOrNull { it.id != camera.id } }
        }
    }

    override fun open() {
        launch(CommonPool) {
            cameraSources.forEach { it.open() }
            if (selectedCamera == null || !selectedCamera!!.isPreviewing()) {
                selectFirst()
            }
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
}