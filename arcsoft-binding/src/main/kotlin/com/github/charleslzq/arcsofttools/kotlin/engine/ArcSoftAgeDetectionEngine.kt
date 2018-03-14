package com.github.charleslzq.arcsofttools.kotlin.engine

import android.util.Log
import com.arcsoft.ageestimation.ASAE_FSDKEngine
import com.arcsoft.ageestimation.ASAE_FSDKError
import com.arcsoft.ageestimation.ASAE_FSDKVersion
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSetting

/**
 * Created by charleslzq on 18-3-2.
 */
class ArcSoftAgeDetectionEngine(
        private val keys: ArcSoftSdkKey,
        private val setting: ArcSoftSetting
) : ArcSoftEngineWrapper<ASAE_FSDKEngine, ASAE_FSDKVersion>() {
    override fun init() = if (setting.useAgeDetection) {
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

    override fun determineVersion(internalEngine: ASAE_FSDKEngine) = ASAE_FSDKVersion().run {
        if (internalEngine.ASAE_FSDK_GetVersion(this).code == ASAE_FSDKError.MOK) {
            this
        } else {
            null
        }
    }

    override fun close() {
        getEngine()?.ASAE_FSDK_UninitAgeEngine()
    }
}