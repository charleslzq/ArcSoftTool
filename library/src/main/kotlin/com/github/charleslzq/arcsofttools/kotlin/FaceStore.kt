package com.github.charleslzq.arcsofttools.kotlin

import com.arcsoft.facerecognition.AFR_FSDKFace

/**
 * Created by charleslzq on 18-2-28.
 */
interface FaceStore {
    fun listNames(): List<String>
    fun load(name: String): FaceData
    fun addFace(name: String, face: AFR_FSDKFace)
    fun delete(name: String)
}