package com.github.charleslzq.arcsofttools.kotlin

import android.util.Log
import com.arcsoft.ageestimation.ASAE_FSDKEngine
import com.arcsoft.ageestimation.ASAE_FSDKError
import com.arcsoft.facedetection.AFD_FSDKEngine
import com.arcsoft.facedetection.AFD_FSDKError
import com.arcsoft.facerecognition.AFR_FSDKEngine
import com.arcsoft.facerecognition.AFR_FSDKError
import com.arcsoft.facetracking.AFT_FSDKEngine
import com.arcsoft.facetracking.AFT_FSDKError
import com.arcsoft.genderestimation.ASGE_FSDKEngine
import com.arcsoft.genderestimation.ASGE_FSDKError
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSetting

/**
 * Created by charleslzq on 18-3-1.
 */
class ArcSoftEngine(keys: ArcSoftSdkKey, val setting: ArcSoftSetting) : AutoCloseable {
    private val faceRecognitionEngine = if (setting.useFaceRecognition) {
        AFR_FSDKEngine().let {
            it.AFR_FSDK_InitialEngine(keys.appId, keys.faceRecognitionKey).run {
                if (code == AFR_FSDKError.MOK) {
                    it
                } else {
                    Log.e(TAG, "Face Recognition Engine fail! error code : $code")
                    null
                }
            }
        }
    } else {
        null
    }
    private val faceDetectEngine = if (setting.useFaceDetection) {
        AFD_FSDKEngine().let {
            it.AFD_FSDK_InitialFaceEngine(
                keys.appId,
                keys.faceDetectionKey,
                AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT,
                setting.scale,
                setting.maxFaceNum
            ).run {
                if (code == AFD_FSDKError.MOK) {
                    it
                } else {
                    Log.e(TAG, "Face Detect Engine fail! error code : $code")
                    null
                }
            }
        }
    } else {
        null
    }
    private val faceTrackEngine = if (setting.useFaceTracking) {
        AFT_FSDKEngine().let {
            it.AFT_FSDK_InitialFaceEngine(
                keys.appId,
                keys.faceTrackingKey,
                AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT,
                setting.scale,
                setting.maxFaceNum
            ).run {
                if (code == AFT_FSDKError.MOK) {
                    it
                } else {
                    Log.e(TAG, "Face Track Engine fail! error code : $code")
                    null
                }
            }
        }
    } else {
        null
    }
    private val ageDetectEngine = if (setting.useAgeDetection) {
        ASAE_FSDKEngine().let {
            it.ASAE_FSDK_InitAgeEngine(keys.appId, keys.ageKey).run {
                if (code == ASAE_FSDKError.MOK) {
                    it
                } else {
                    Log.e(TAG, "Age Detect Engine fail! error code : $code")
                    null
                }
            }
        }
    } else {
        null
    }
    private val genderDetectEngine = if (setting.useGenderDetection) {
        ASGE_FSDKEngine().let {
            it.ASGE_FSDK_InitgGenderEngine(keys.appId, keys.genderKey).run {
                if (code == ASGE_FSDKError.MOK) {
                    it
                } else {
                    Log.e(TAG, "Gender Detect Engine fail! error code : $code")
                    null
                }
            }
        }
    } else {
        null
    }

    override fun close() {
        faceRecognitionEngine?.AFR_FSDK_UninitialEngine()
        faceDetectEngine?.AFD_FSDK_UninitialFaceEngine()
        faceTrackEngine?.AFT_FSDK_UninitialFaceEngine()
        ageDetectEngine?.ASAE_FSDK_UninitAgeEngine()
        genderDetectEngine?.ASGE_FSDK_UninitGenderEngine()
    }

    companion object {
        const val TAG = "ArcSoftEngine"
    }
}