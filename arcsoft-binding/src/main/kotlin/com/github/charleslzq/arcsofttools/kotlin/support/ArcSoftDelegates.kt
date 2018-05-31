package com.github.charleslzq.arcsofttools.kotlin.support

import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceOfflineEngine
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftOfflineEngineAdapterBase
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.faceengine.core.FaceOfflineEngineRxDelegate
import com.github.charleslzq.faceengine.support.callOnCompute
import com.github.charleslzq.faceengine.view.CameraPreview
import com.github.charleslzq.facestore.ReadWriteFaceStore

/**
 * Created by charleslzq on 18-3-14.
 */
class ArcSoftRxDelegate<S : ArcSoftSetting, D : ReadWriteFaceStore<Person, Face>>(
        arcSoftEngineAdapterBase: ArcSoftOfflineEngineAdapterBase<S, D>
) : FaceOfflineEngineRxDelegate<CameraPreview.PreviewFrame, Person, Face, Float, D>(arcSoftEngineAdapterBase), ArcSoftFaceOfflineEngine<D> {
    override fun detectGender(image: CameraPreview.PreviewFrame) = callOnCompute { (delegate as ArcSoftOfflineEngineAdapterBase<*, *>).detectGender(image) }
    override fun detectAge(image: CameraPreview.PreviewFrame) = callOnCompute { (delegate as ArcSoftOfflineEngineAdapterBase<*, *>).detectAge(image) }
    override fun trackFace(image: CameraPreview.PreviewFrame) = callOnCompute { (delegate as ArcSoftOfflineEngineAdapterBase<*, *>).trackFace(image) }
}