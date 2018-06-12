package com.github.charleslzq.faceengine.view

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.view.config.*
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.view.CameraView
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by charleslzq on 18-3-8.
 */
val CameraView.textureView
    get() = getChildAt(0) as TextureView

fun CameraPreviewOperator.isFoto() = this is FotoCameraOperatorSource.FotoCameraPreviewOperator

class FaceDetectView
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0) :
        FrameLayout(context, attributeSet, defStyle), CameraSource {
    private val settingManager = CameraSettingManager(context)
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
                    startPreview(settingManager.loadRequest(source.id, id, this is FotoCameraOperatorSource.FotoCameraPreviewOperator))
                }
            }
        }
    val selectedCamera: CameraPreviewOperator?
        get() = selectCamera(cameras)

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

    fun withDeviceInfo(consumer: Consumer<List<CameraPreviewOperator.Info>>) = withDeviceInfo { consumer.consume(it) }

    fun withDeviceInfo(process: (List<CameraPreviewOperator.Info>) -> Unit) {
        Single.fromCallable {
            cameras.map {
                val latch = CountDownLatch(1)
                var cameraInfo: CameraPreviewOperator.Info? = null
                if (it.isFoto()) {
                    it.startPreview(FotoCameraPreviewRequest())
                }
                it.withCameraInfo().subscribe { result, _ ->
                    cameraInfo = result
                    latch.countDown()
                }
                latch.await()
                if (it.isFoto()) {
                    it.stopPreview()
                }
                cameraInfo!!
            }
        }.subscribeOn(Schedulers.io()).doAfterSuccess { restart() }.subscribe(process)
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
        cameraSources.forEach { it.open() }
        if (selectedCamera == null || !selectedCamera!!.isPreviewing()) {
            selectFirst()
        }
    }

    fun restart() {
        selectedCamera?.run {
            stopPreview()
            startPreview(settingManager.loadRequest(source.id, id, this is FotoCameraOperatorSource.FotoCameraPreviewOperator))
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

    fun savePreviewConfig(camerePreviewConfiguration: CameraPreviewConfiguration) {
        settingManager.savePreviewConfig(cameraPreviewConfiguration)
    }

    fun loadRequest(cameraPreviewOperator: CameraPreviewOperator) = cameraPreviewOperator.run {
        settingManager.loadRequest(source.id, id, this is FotoCameraOperatorSource.FotoCameraPreviewOperator)
    }

    fun saveRequest(cameraPreviewOperator: CameraPreviewOperator, request: CameraPreviewRequest) = cameraPreviewOperator.run {
        if ((isFoto() && request !is FotoCameraPreviewRequest)
                || (!isFoto() && request !is UVCCameraPreviewRequest)) {
            throw IllegalArgumentException("Wrong types of config")
        }
        settingManager.configFor(source.id, id, request)
    }
}