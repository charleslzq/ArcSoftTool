package com.github.charleslzq.faceengine.view.config

import com.github.charleslzq.faceengine.view.CameraPreviewOperator
import com.github.charleslzq.faceengine.view.FotoCameraOperatorSource
import com.github.charleslzq.faceengine.view.Selector
import com.github.charleslzq.faceengine.view.UVCCameraOperatorSource
import io.fotoapparat.parameter.Resolution

fun CameraPreviewOperator.getDefaultRequest() = CameraPreviewRequest.getDefaultRequest(this)

sealed class CameraPreviewRequest {
    abstract val resolutionSelector: ResolutionSelector

    companion object {
        @JvmStatic
        fun getDefaultRequest(cameraPreviewOperator: CameraPreviewOperator) = when (cameraPreviewOperator) {
            is FotoCameraOperatorSource.FotoCameraPreviewOperator -> FotoCameraPreviewRequest()
            is UVCCameraOperatorSource.UVCCameraOperator -> UVCCameraPreviewRequest()
            else -> throw IllegalArgumentException("Unsupported camera operator type")
        }
    }
}

data class FotoCameraPreviewRequest(
        override val resolutionSelector: ResolutionSelector = ResolutionSelector.MaxWidth
) : CameraPreviewRequest()

data class UVCCameraPreviewRequest(
        override val resolutionSelector: ResolutionSelector = ResolutionSelector.MaxWidth
) : CameraPreviewRequest()

sealed class ResolutionSelector {
    abstract val instance: (Iterable<Resolution>) -> Resolution?

    object MaxArea : ResolutionSelector() {
        override val instance: (Iterable<Resolution>) -> Resolution?
            get() = { it.maxBy { it.area } }
    }

    object MinArea : ResolutionSelector() {
        override val instance: (Iterable<Resolution>) -> Resolution?
            get() = { it.minBy { it.area } }
    }

    object MaxWidth : ResolutionSelector() {
        override val instance: (Iterable<Resolution>) -> Resolution?
            get() = { it.maxBy { it.width } }
    }

    object MinWidth : ResolutionSelector() {
        override val instance: (Iterable<Resolution>) -> Resolution?
            get() = { it.minBy { it.width } }
    }

    object MaxHeight : ResolutionSelector() {
        override val instance: (Iterable<Resolution>) -> Resolution?
            get() = { it.maxBy { it.height } }
    }

    object MinHeight : ResolutionSelector() {
        override val instance: (Iterable<Resolution>) -> Resolution?
            get() = { it.minBy { it.height } }
    }

    class Match(val target: Resolution) : ResolutionSelector() {
        override val instance: (Iterable<Resolution>) -> Resolution?
            get() = { it.firstOrNull { it == target } }
    }

    class Custom(
            override val instance: (Iterable<Resolution>) -> Resolution?
    ) : ResolutionSelector() {
        constructor(selector: Selector<Resolution>) : this({
            selector.select(it)
        })
    }
}