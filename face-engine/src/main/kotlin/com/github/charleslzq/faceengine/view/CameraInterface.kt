package com.github.charleslzq.faceengine.view

import com.github.charleslzq.faceengine.core.TrackedFace
import io.fotoapparat.parameter.Resolution
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

interface CameraLifeCycle {
    fun onResume()
    fun onPause()
    fun onDestroy()
    fun isRunning(): Boolean
}

interface CameraWithFaceTrack {
    fun updateTrackFaces(faces: List<TrackedFace>)
}

interface CameraPreview {
    fun onPreviewFrame(
            scheduler: Scheduler = AndroidSchedulers.mainThread(),
            processor: (PreviewFrame) -> Unit
    ): Disposable

    fun onPreviewFrame(
            scheduler: Scheduler = AndroidSchedulers.mainThread(),
            frameConsumer: FrameConsumer
    ): Disposable

    data class PreviewFrame(
            val size: Resolution,
            val image: ByteArray,
            val rotation: Int
    )

    @FunctionalInterface
    interface FrameConsumer {
        fun accept(previewFrame: PreviewFrame)
    }
}

interface CameraInterface : CameraLifeCycle, CameraWithFaceTrack, CameraPreview {
    fun hasAvailableCamera(): Boolean
}