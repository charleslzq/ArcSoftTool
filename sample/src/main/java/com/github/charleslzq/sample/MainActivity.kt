package com.github.charleslzq.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSetting

class MainActivity : AppCompatActivity() {

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
            Log.d("test", "fd $useFaceDetection")
            Log.d("test", "fr $useFaceRecognition")
            Log.d("test", "ft $useFaceTracking")
            Log.d("test", "al $allowRegister")
            Log.d("test", faceDirectory)
        }

    }
}
