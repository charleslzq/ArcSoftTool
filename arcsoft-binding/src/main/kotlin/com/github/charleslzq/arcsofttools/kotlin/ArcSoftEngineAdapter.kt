package com.github.charleslzq.arcsofttools.kotlin

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Environment
import com.arcsoft.facedetection.AFD_FSDKEngine
import com.arcsoft.facedetection.AFD_FSDKError
import com.arcsoft.facedetection.AFD_FSDKFace
import com.arcsoft.facerecognition.AFR_FSDKEngine
import com.arcsoft.facerecognition.AFR_FSDKError
import com.arcsoft.facerecognition.AFR_FSDKFace
import com.arcsoft.facerecognition.AFR_FSDKMatching
import com.github.charleslzq.arcsofttools.kotlin.engine.*
import com.github.charleslzq.faceengine.core.kotlin.FaceEngine
import com.github.charleslzq.faceengine.core.kotlin.FaceEngineRxDelegate
import com.github.charleslzq.faceengine.core.kotlin.FaceEngineServiceBackground
import com.github.charleslzq.faceengine.core.kotlin.store.FaceFileStore
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStore
import com.github.charleslzq.faceengine.core.kotlin.store.ReadWriteFaceStoreRxDelegate
import com.guo.android_extend.image.ImageConverter
import java.util.*

/**
 * Created by charleslzq on 18-3-1.
 */
class ArcSoftEngineAdapter(keys: ArcSoftSdkKey, setting: ArcSoftSetting) : AutoCloseable,
    FaceEngine<Person, Face, Float, ReadWriteFaceStore<Person, Face>> {
    override val store: ReadWriteFaceStoreRxDelegate<Person, Face, ReadWriteFaceStore<Person, Face>> =
        ReadWriteFaceStoreRxDelegate(
            FaceFileStore(
                Environment.getExternalStorageDirectory().absolutePath + setting.faceDirectory,
                ArcSoftFaceDataType()
            )
        )
    val faceRecognitionEngine = ArcSoftFaceRecognitionEngine(keys)
    val faceDetectEngine = ArcSoftFaceDetectionEngine(keys, setting)
    val faceTrackEngine = ArcSoftFaceTrackingEngine(keys, setting)
    val ageDetectEngine = ArcSoftAgeDetectionEngine(keys, setting)
    val genderDetectEngine = ArcSoftGenderDetectionEngine(keys, setting)

    override fun detect(image: Bitmap): List<Face> {
        return if (faceDetectEngine.getEngine() != null && faceRecognitionEngine.getEngine() != null) {
            val detectResult = mutableListOf<AFD_FSDKFace>()
            val data = ByteArray(image.width * image.height * 3 / 2).apply {
                ImageConverter().run {
                    initial(image.width, image.height, ImageConverter.CP_PAF_NV21)
                    convert(image, this@apply)
                    destroy()
                }
            }
            val detectCode = faceDetectEngine.getEngine()!!.AFD_FSDK_StillImageFaceDetection(
                data,
                image.width,
                image.height,
                AFD_FSDKEngine.CP_PAF_NV21,
                detectResult
            )
            if (detectCode.code == AFD_FSDKError.MOK) {
                val recognitionVersion = faceRecognitionEngine.getVersion()!!
                detectResult.mapNotNull {
                    val extractResult = AFR_FSDKFace()
                    val extractCode = faceRecognitionEngine.getEngine()!!.AFR_FSDK_ExtractFRFeature(
                        data, image.width, image.height,
                        AFR_FSDKEngine.CP_PAF_NV21,
                        Rect(it.rect),
                        it.degree,
                        extractResult
                    )
                    if (extractCode.code == AFR_FSDKError.MOK) {
                        Face(UUID.randomUUID().toString(), image, extractResult, recognitionVersion)
                    } else {
                        null
                    }
                }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override fun calculateSimilarity(savedFace: Face, newFace: Face): Float {
        return if (faceRecognitionEngine.getEngine() != null) {
            AFR_FSDKMatching().let {
                val compareCode = faceRecognitionEngine.getEngine()!!.AFR_FSDK_FacePairMatching(
                    newFace.data,
                    savedFace.data,
                    it
                )
                if (compareCode.code == AFR_FSDKError.MOK) {
                    it.score
                } else {
                    -1.0f
                }
            }
        } else {
            -1.0f
        }
    }

    override fun close() {
        faceRecognitionEngine.close()
        faceDetectEngine.close()
        faceTrackEngine.close()
        ageDetectEngine.close()
        genderDetectEngine.close()
    }

    companion object {
        @JvmStatic
        fun createEngine(resources: Resources) =
            FaceEngineRxDelegate(ArcSoftEngineAdapter(ArcSoftSdkKey(), ArcSoftSetting(resources)))
    }
}

class ArcSoftEngineService :
    FaceEngineServiceBackground<Person, Face, Float, ReadWriteFaceStore<Person, Face>>() {
    override fun createEngine() = ArcSoftEngineAdapter.createEngine(resources)
}