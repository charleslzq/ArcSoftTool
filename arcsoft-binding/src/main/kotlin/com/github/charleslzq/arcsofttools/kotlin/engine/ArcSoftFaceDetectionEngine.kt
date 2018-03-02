package com.github.charleslzq.arcsofttools.kotlin.engine

import android.util.Log
import com.arcsoft.facedetection.AFD_FSDKEngine
import com.arcsoft.facedetection.AFD_FSDKError
import com.arcsoft.facedetection.AFD_FSDKVersion
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSetting

/**
 * Created by charleslzq on 18-3-2.
 */
class ArcSoftFaceDetectionEngineprivate(
    private val keys: ArcSoftSdkKey,
    private val setting: ArcSoftSetting
) : ArcSoftEngineWrapper<AFD_FSDKEngine, AFD_FSDKVersion>() {
    override fun init() = if (setting.useFaceDetection) {
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

    override fun determineVersion(internalEngine: AFD_FSDKEngine) = AFD_FSDKVersion().run {
        if (internalEngine.AFD_FSDK_GetVersion(this).code == AFD_FSDKError.MOK) {
            this
        } else {
            null
        }
    }

    override fun close() {
        getEngine()?.AFD_FSDK_UninitialFaceEngine()
    }
}