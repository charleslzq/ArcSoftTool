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
    val allowRegister by BooleanXMLResource(resources, R.bool.ArcSoft_allowRegister)
    val faceDirectory by StringXMLResource(resources, R.string.ArcSoft_faceDirectory)
}