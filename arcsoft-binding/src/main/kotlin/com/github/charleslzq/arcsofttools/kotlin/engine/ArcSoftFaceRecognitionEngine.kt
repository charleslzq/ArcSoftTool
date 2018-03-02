package com.github.charleslzq.arcsofttools.kotlin.engine

import android.util.Log
import com.arcsoft.facerecognition.AFR_FSDKEngine
import com.arcsoft.facerecognition.AFR_FSDKError
import com.arcsoft.facerecognition.AFR_FSDKVersion
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSdkKey

/**
 * Created by charleslzq on 18-3-2.
 */
class ArcSoftFaceRecognitionEngine(
    private val keys: ArcSoftSdkKey
) : ArcSoftEngineWrapper<AFR_FSDKEngine, AFR_FSDKVersion>() {
    override fun init() =
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

    override fun determineVersion(internalEngine: AFR_FSDKEngine) = AFR_FSDKVersion().run {
        if (internalEngine.AFR_FSDK_GetVersion(this).code == AFR_FSDKError.MOK) {
            this
        } else {
            null
        }
    }

    override fun close() {
        getEngine()?.AFR_FSDK_UninitialEngine()
    }
}