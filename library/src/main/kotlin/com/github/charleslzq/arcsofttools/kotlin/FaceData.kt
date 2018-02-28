package com.github.charleslzq.arcsofttools.kotlin

import com.arcsoft.facerecognition.AFR_FSDKFace

/**
 * Created by charleslzq on 18-2-28.
 */
data class FaceData(val name: String, val faces: MutableList<AFR_FSDKFace>)