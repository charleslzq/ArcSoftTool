package com.github.charleslzq.arcsofttools.kotlin.support

import com.github.charleslzq.arcsofttools.kotlin.ArcSoftEngineAdapterBase
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceEngine
import com.github.charleslzq.arcsofttools.kotlin.Face
import com.github.charleslzq.arcsofttools.kotlin.Person
import com.github.charleslzq.faceengine.core.FaceEngineRxDelegate
import com.github.charleslzq.faceengine.support.callOnCompute
import com.github.charleslzq.facestore.ReadWriteFaceStore
import io.fotoapparat.preview.Frame

/**
 * Created by charleslzq on 18-3-14.
 */
class ArcSoftRxDelegate<S : ArcSoftSetting, out D : ReadWriteFaceStore<Person, Face>>(
        arcSoftEngineAdapterBase: ArcSoftEngineAdapterBase<S, D>
) : FaceEngineRxDelegate<Frame, Person, Face, Float, D>(arcSoftEngineAdapterBase), ArcSoftFaceEngine<D> {
    override fun detectAge(image: Frame) = callOnCompute { (delegate as ArcSoftEngineAdapterBase<*, *>).detectAge(image) }
}