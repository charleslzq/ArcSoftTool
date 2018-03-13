package com.github.charleslzq.arcsofttools.kotlin.engine

import android.util.Log
import com.arcsoft.facetracking.AFT_FSDKEngine
import com.arcsoft.facetracking.AFT_FSDKError
import com.arcsoft.facetracking.AFT_FSDKVersion
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSetting

/**
 * Created by charleslzq on 18-3-2.
 */
class ArcSoftFaceTrackingEngine(
        private val keys: ArcSoftSdkKey,
        private val setting: ArcSoftSetting
) : ArcSoftEngineWrapper<AFT_FSDKEngine, AFT_FSDKVersion>() {
    override fun init() = if (setting.useFaceTracking) {
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

    override fun determineVersion(internalEngine: AFT_FSDKEngine) = AFT_FSDKVersion().run {
        if (internalEngine.AFT_FSDK_GetVersion(this).code == AFT_FSDKError.MOK) {
            this
        } else {
            null
        }
    }

    override fun close() {
        getEngine()?.AFT_FSDK_UninitialFaceEngine()
    }
}