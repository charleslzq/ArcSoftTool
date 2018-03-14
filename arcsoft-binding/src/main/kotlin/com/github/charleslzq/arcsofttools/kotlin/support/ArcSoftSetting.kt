package com.github.charleslzq.arcsofttools.kotlin.support

import android.content.res.Resources
import com.github.charleslzq.arcsofttools.R
import com.github.charleslzq.faceengine.support.BooleanXMLResource
import com.github.charleslzq.faceengine.support.IntXMLResource
import com.github.charleslzq.faceengine.support.StringXMLResource

/**
 * Created by charleslzq on 18-2-28.
 */
open class ArcSoftSetting(resources: Resources) {
    val useFaceTracking by BooleanXMLResource(
            resources,
            R.bool.ArcSoft_useFaceTracking
    )
    val useAgeDetection by BooleanXMLResource(
            resources,
            R.bool.ArcSoft_useAgeDetection
    )
    val useGenderDetection by BooleanXMLResource(
            resources,
            R.bool.ArcSoft_useGenderDetection
    )
    val faceDirectory by StringXMLResource(
            resources,
            R.string.ArcSoft_faceDirectory
    )
    val scale by IntXMLResource(
            resources,
            R.integer.ArcSoft_scale
    )
    val maxFaceNum by IntXMLResource(
            resources,
            R.integer.ArcSoft_maxFaceNum
    )
}