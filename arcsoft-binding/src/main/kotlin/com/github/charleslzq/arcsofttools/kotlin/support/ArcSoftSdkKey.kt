package com.github.charleslzq.arcsofttools.kotlin.support

import com.github.charleslzq.arcsofttools.BuildConfig

/**
 * Created by charleslzq on 18-2-28.
 */
// todo BuildConfig需要加密
class ArcSoftSdkKey {
    val appId = BuildConfig.ARCSOFT_APPID
    val faceTrackingKey = BuildConfig.ARCSOFT_FACE_TRACKING_KEY
    val faceDetectionKey = BuildConfig.ARCSOFT_FACE_DETECTION_KEY
    val faceRecognitionKey = BuildConfig.ARCSOFT_FACE_RECOGNITION_KEY
    val ageKey = BuildConfig.ARCSOFT_AGE_KEY
    val genderKey = BuildConfig.ARCSOFT_GENDER_KEY
}