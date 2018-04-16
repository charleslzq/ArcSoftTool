package com.github.charleslzq.faceengine.view

import com.github.charleslzq.faceengine.core.TrackedFace
import io.fotoapparat.parameter.Resolution
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

interface CameraLifeCycle {
    fun onResume()
    fun onPause()
    fun onDestroy()
    fun isRunning(): Boolean
}

interface CameraWithFaceTrack {
    fun updateTrackFaces(faces: Collection<TrackedFace>)
}

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

interface CameraInterface : CameraLifeCycle, CameraWithFaceTrack, CameraPreview {
    fun hasAvailableCamera(): Boolean
}