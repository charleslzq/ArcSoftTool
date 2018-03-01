package com.github.charleslzq.arcsofttools.kotlin.support

import android.content.res.Resources
import com.github.charleslzq.arcsofttools.R

/**
 * Created by charleslzq on 18-2-28.
 */
class ArcSoftSetting(resources: Resources) {
    val useFaceTracking by BooleanXMLResource(resources, R.bool.ArcSoft_useFaceTracking)
    val useFaceDetection by BooleanXMLResource(resources, R.bool.ArcSoft_useFaceDetection)
    val useFaceRecognition by BooleanXMLResource(resources, R.bool.ArcSoft_useFaceRecognition)
    val useAgeDetection by BooleanXMLResource(resources, R.bool.ArcSoft_useAgeDetection)
    val useGenderDetection by BooleanXMLResource(resources, R.bool.ArcSoft_useGenderDetection)
    val faceDirectory by StringXMLResource(resources, R.string.ArcSoft_faceDirectory)
    val scale by IntXMLResource(resources, R.integer.ArcSoft_scale)
    val maxFaceNum by IntXMLResource(resources, R.integer.ArcSoft_maxFaceNum)
}