package com.github.charleslzq.arcsofttools.kotlin.support

import android.content.Context
import java.util.*

/**
 * Created by charleslzq on 18-2-28.
 */
data class ArcSoftSdkKey(
        val appId: String = "",
        val faceTrackingKey: String = "",
        val faceDetectionKey: String = "",
        val faceRecognitionKey: String = "",
        val ageKey: String = "",
        val genderKey: String = ""
) {
    companion object {
        fun read(context: Context, fileName: String = "arcsoft.keys") = Properties().run {
            context.assets.open(fileName).use {
                load(it).run {
                    ArcSoftSdkKey(
                            getProperty("ArcSoft.appId"),
                            getProperty("ArcSoft.faceTrackingKey"),
                            getProperty("ArcSoft.faceDetectionKey"),
                            getProperty("ArcSoft.faceRecognitionKey"),
                            getProperty("ArcSoft.ageKey"),
                            getProperty("ArcSoft.genderKey")
                    )
                }
            }
        }
    }
}