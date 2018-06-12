package com.github.charleslzq.faceengine.view.config

import com.github.charleslzq.faceengine.view.Selector
import io.fotoapparat.parameter.Resolution

sealed class CameraPreviewRequest {
    abstract val resolutionSelector: ResolutionSelector
}

data class FotoCameraPreviewRequest(
        override val resolutionSelector: ResolutionSelector = ResolutionSelector.MaxWidth
) : CameraPreviewRequest()

data class UvcCameraPreviewRequest(
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