package com.github.charleslzq.faceengine.view

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.widget.FrameLayout
import com.github.charleslzq.faceengine.core.TrackedFace
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable

/**
 * Created by charleslzq on 18-3-8.
 */
class FaceDetectView
@JvmOverloads
constructor(context: Context, attributeSet: AttributeSet? = null, @AttrRes defStyle: Int = 0) :
        FrameLayout(context, attributeSet, defStyle), CameraInterface {
    private val viewList = listOf<CameraInterface>(
            UVCCameraAdapter(context, attributeSet, defStyle).also { addView(it) },
            FotoCameraViewAdapter(context, attributeSet, defStyle).also { addView(it) }
    )
    private var select: (Iterable<CameraInterface>) -> CameraInterface = {
        it.first { it.hasAvailableCamera() }
    }
    private val disposables = mutableListOf<Disposable>()

    override fun onResume() {
        onPause()
        select(viewList).onResume()
    }

    override fun onPause() {
        viewList.forEach { it.onPause() }
    }

    override fun onDestroy() {
        viewList.forEach { it.onDestroy() }
    }

    override fun isRunning() = select(viewList).isRunning()

    override fun updateTrackFaces(faces: List<TrackedFace>) = select(viewList).updateTrackFaces(faces)

    override fun hasAvailableCamera() = viewList.any { it.hasAvailableCamera() }

    override fun onPreviewFrame(scheduler: Scheduler, processor: (CameraPreview.PreviewFrame) -> Unit): Disposable {
        disposables.filter { !it.isDisposed }.forEach { it.dispose() }
        disposables.clear()
        return select(viewList).onPreviewFrame(scheduler, processor).apply { disposables.add(this) }
    }

    override fun onPreviewFrame(scheduler: Scheduler, frameConsumer: CameraPreview.FrameConsumer): Disposable {
        return onPreviewFrame(scheduler) { frameConsumer.accept(it) }
    }

    companion object {
        const val TAG = "FaceDetectView"
    }
}