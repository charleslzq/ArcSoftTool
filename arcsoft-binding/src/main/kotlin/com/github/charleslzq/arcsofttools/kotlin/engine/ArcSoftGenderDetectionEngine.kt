package com.github.charleslzq.arcsofttools.kotlin.engine

import android.util.Log
import com.arcsoft.genderestimation.ASGE_FSDKEngine
import com.arcsoft.genderestimation.ASGE_FSDKError
import com.arcsoft.genderestimation.ASGE_FSDKVersion
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSetting

/**
 * Created by charleslzq on 18-3-2.
 */
class ArcSoftGenderDetectionEngine(
        private val keys: ArcSoftSdkKey,
        private val setting: ArcSoftSetting
) : ArcSoftEngineWrapper<ASGE_FSDKEngine, ASGE_FSDKVersion>() {
    override fun init() = if (setting.useGenderDetection) {
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

    override fun determineVersion(internalEngine: ASGE_FSDKEngine) = ASGE_FSDKVersion().run {
        if (internalEngine.ASGE_FSDK_GetVersion(this).code == ASGE_FSDKError.MOK) {
            this
        } else {
            null
        }
    }

    override fun close() {
        getEngine()?.ASGE_FSDK_UninitGenderEngine()
    }
}