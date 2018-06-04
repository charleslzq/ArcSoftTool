package com.github.charleslzq.arcsofttools.kotlin.support

import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceOfflineEngine
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftOfflineEngineAdapterBase
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.faceengine.core.FaceOfflineEngineRxDelegate
import com.github.charleslzq.faceengine.support.callOnCompute
import com.github.charleslzq.faceengine.view.PreviewFrame
import com.github.charleslzq.facestore.ReadWriteFaceStore

/**
 * Created by charleslzq on 18-3-14.
 */
class ArcSoftRxDelegate<S : ArcSoftSetting, D : ReadWriteFaceStore<Person, Face>>(
        arcSoftEngineAdapterBase: ArcSoftOfflineEngineAdapterBase<S, D>
) : FaceOfflineEngineRxDelegate<PreviewFrame, Person, Face, Float, D>(arcSoftEngineAdapterBase), ArcSoftFaceOfflineEngine<D> {
    override fun detectGender(image: PreviewFrame) = callOnCompute { (delegate as ArcSoftOfflineEngineAdapterBase<*, *>).detectGender(image) }
    override fun detectAge(image: PreviewFrame) = callOnCompute { (delegate as ArcSoftOfflineEngineAdapterBase<*, *>).detectAge(image) }
    override fun trackFace(image: PreviewFrame) = callOnCompute { (delegate as ArcSoftOfflineEngineAdapterBase<*, *>).trackFace(image) }
}