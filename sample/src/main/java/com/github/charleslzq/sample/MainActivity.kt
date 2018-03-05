package com.github.charleslzq.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftEngineAdapter
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceDataType
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSetting
import com.github.charleslzq.faceengine.core.kotlin.FaceDetectionEngineRxDelegate
import com.github.charleslzq.faceengine.core.kotlin.FaceEngine
import com.github.charleslzq.faceengine.core.kotlin.FaceRecognitionEngineRxDelegate
import com.github.charleslzq.faceengine.core.kotlin.store.FaceFileStore
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStoreRxDelegate

class MainActivity : AppCompatActivity() {

    val engine by lazy {
        val keys = ArcSoftSdkKey()
        val setting = ArcSoftSetting(resources)
        val store = FaceFileStore(setting.faceDirectory, ArcSoftFaceDataType())
        val adapter = ArcSoftEngineAdapter(keys, setting)
        FaceEngine(
            ReadWriteFaceStoreRxDelegate(store),
            FaceDetectionEngineRxDelegate(adapter),
            FaceRecognitionEngineRxDelegate(adapter)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ArcSoftSdkKey().apply {
            Log.d("test", appId)
            Log.d("test", faceDetectionKey)
            Log.d("test", faceRecognitionKey)
            Log.d("test", faceTrackingKey)
            Log.d("test", ageKey)
            Log.d("test", genderKey)
        }
        ArcSoftSetting(resources).apply {
            Log.d("test", "ft $useFaceTracking")
            Log.d("test", "al $useAgeDetection")
            Log.d("test", faceDirectory)
        }
    }
}
