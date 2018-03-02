package com.github.charleslzq.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftEngineBinder
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftSetting
import com.github.charleslzq.faceengine.core.kotlin.FaceEngine

class MainActivity : AppCompatActivity() {

    val engine = FaceEngine(ArcSoftEngineBinder(ArcSoftSdkKey(), ArcSoftSetting(resources)))

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
