package com.github.charleslzq.arcsofttools.kotlin

import com.github.charleslzq.faceengine.core.kotlin.FaceEngineBinder
import com.github.charleslzq.faceengine.core.kotlin.store.FaceFileStore

/**
 * Created by charleslzq on 18-3-2.
 */
class ArcSoftEngineBinder(keys: ArcSoftSdkKey, setting: ArcSoftSetting) :
    FaceEngineBinder<Person, Face, Float> {
    private val store = FaceFileStore(setting.faceDirectory, ArcSoftFaceDataType())
    val adapter = ArcSoftEngineAdapter(keys, setting)

    override fun createStore() = store

    override fun createFaceRecognitionEngine() = adapter

    override fun createFaceDetectionEngine() = adapter
}