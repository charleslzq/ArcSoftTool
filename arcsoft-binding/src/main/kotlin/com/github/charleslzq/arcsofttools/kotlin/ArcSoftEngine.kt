package com.github.charleslzq.arcsofttools.kotlin

import com.github.charleslzq.arcsofttools.kotlin.engine.*

/**
 * Created by charleslzq on 18-3-1.
 */
class ArcSoftEngine(keys: ArcSoftSdkKey, setting: ArcSoftSetting) : AutoCloseable {
    private val faceRecognitionEngine = ArcSoftFaceRecognitionEngine(keys, setting)
    private val faceDetectEngine = ArcSoftFaceDetectionEngineprivate(keys, setting)
    private val faceTrackEngine = ArcSoftFaceTrackingEngine(keys, setting)
    private val ageDetectEngine = ArcSoftAgeDetectionEngine(keys, setting)
    private val genderDetectEngine = ArcSoftGenderDetectionEngine(keys, setting)

    override fun close() {
        faceRecognitionEngine.close()
        faceDetectEngine.close()
        faceTrackEngine.close()
        ageDetectEngine.close()
        genderDetectEngine.close()
    }
}