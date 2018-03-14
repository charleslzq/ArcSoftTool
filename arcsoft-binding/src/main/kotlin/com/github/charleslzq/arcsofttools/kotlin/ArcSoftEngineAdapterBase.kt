package com.github.charleslzq.arcsofttools.kotlin

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
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSetting
import com.github.charleslzq.faceengine.core.FaceEngine
import com.github.charleslzq.faceengine.core.FaceEngineRxDelegate
import com.github.charleslzq.faceengine.core.FaceEngineService
import com.github.charleslzq.faceengine.core.FaceEngineServiceBackground
import com.github.charleslzq.faceengine.store.ReadWriteFaceStoreCacheDelegate
import com.github.charleslzq.faceengine.store.ReadWriteFaceStoreRxDelegate
import com.github.charleslzq.faceengine.support.toBitmap
import com.github.charleslzq.facestore.FaceFileReadWriteStore
import com.github.charleslzq.facestore.ReadWriteFaceStore
import io.fotoapparat.preview.Frame
import java.util.*

/**
 * Created by charleslzq on 18-3-1.
 */
open class ArcSoftEngineAdapterBase<S : ArcSoftSetting, out D : ReadWriteFaceStore<Person, Face>>(
        keys: ArcSoftSdkKey,
        setting: S,
        createStore: (S) -> D
) : AutoCloseable, FaceEngine<Frame, Person, Face, Float, ReadWriteFaceStore<Person, Face>> {
    final override val store = createStore(setting)
    val faceRecognitionEngine = ArcSoftFaceRecognitionEngine(keys)
    val faceDetectEngine = ArcSoftFaceDetectionEngine(keys, setting)
    val faceTrackEngine = ArcSoftFaceTrackingEngine(keys, setting)
    val ageDetectEngine = ArcSoftAgeDetectionEngine(keys, setting)
    val genderDetectEngine = ArcSoftGenderDetectionEngine(keys, setting)

    final override fun detect(image: Frame): List<Face> {
        return if (faceDetectEngine.getEngine() != null && faceRecognitionEngine.getEngine() != null) {
            val detectResult = mutableListOf<AFD_FSDKFace>()
            val pic = toBitmap(image)
            val detectCode = faceDetectEngine.getEngine()!!.AFD_FSDK_StillImageFaceDetection(
                    image.image,
                    image.size.width,
                    image.size.height,
                    AFD_FSDKEngine.CP_PAF_NV21,
                    detectResult
            )
            if (detectCode.code == AFD_FSDKError.MOK) {
                val recognitionVersion = faceRecognitionEngine.getVersion()!!
                detectResult.mapNotNull {
                    val extractResult = AFR_FSDKFace()
                    val extractCode = faceRecognitionEngine.getEngine()!!.AFR_FSDK_ExtractFRFeature(
                            image.image,
                            image.size.width,
                            image.size.height,
                            AFR_FSDKEngine.CP_PAF_NV21,
                            Rect(it.rect),
                            it.degree,
                            extractResult
                    )
                    if (extractCode.code == AFR_FSDKError.MOK) {
                        Face(UUID.randomUUID().toString(), pic, extractResult, recognitionVersion)
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

    final override fun calculateSimilarity(savedFace: Face, newFace: Face) =
            if (faceRecognitionEngine.getEngine() != null) {
                AFR_FSDKMatching().let {
                    val compareCode = faceRecognitionEngine.getEngine()!!.AFR_FSDK_FacePairMatching(
                            newFace.data,
                            savedFace.data,
                            it
                    )
                    if (compareCode.code == AFR_FSDKError.MOK) {
                        it.score
                    } else {
                        0f
                    }
                }
            } else {
                0f
            }

    final override fun close() {
        faceRecognitionEngine.close()
        faceDetectEngine.close()
        faceTrackEngine.close()
        ageDetectEngine.close()
        genderDetectEngine.close()
    }
}

class DefaultArcSoftEngineService :
        FaceEngineServiceBackground<Frame, Person, Face, Float, ReadWriteFaceStore<Person, Face>>() {
    override fun createEngineService() =
            FaceEngineService(
                    FaceEngineRxDelegate(ArcSoftEngineAdapterBase(ArcSoftSdkKey(), ArcSoftSetting(resources)) {
                        ReadWriteFaceStoreCacheDelegate(
                                ReadWriteFaceStoreRxDelegate(
                                        FaceFileReadWriteStore(
                                                Environment.getExternalStorageDirectory().absolutePath + it.faceDirectory.run {
                                                    if (startsWith('/')) {
                                                        this
                                                    } else {
                                                        "/$this"
                                                    }
                                                },
                                                ArcSoftFaceDataType()
                                        )
                                )
                        )
                    })
            )
}