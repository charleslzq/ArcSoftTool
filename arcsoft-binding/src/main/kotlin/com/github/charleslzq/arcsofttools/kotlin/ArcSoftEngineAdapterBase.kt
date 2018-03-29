package com.github.charleslzq.arcsofttools.kotlin

import android.graphics.Rect
import android.os.Environment
import com.arcsoft.ageestimation.ASAE_FSDKAge
import com.arcsoft.ageestimation.ASAE_FSDKError
import com.arcsoft.ageestimation.ASAE_FSDKFace
import com.arcsoft.facedetection.AFD_FSDKEngine
import com.arcsoft.facedetection.AFD_FSDKError
import com.arcsoft.facedetection.AFD_FSDKFace
import com.arcsoft.facerecognition.AFR_FSDKEngine
import com.arcsoft.facerecognition.AFR_FSDKError
import com.arcsoft.facerecognition.AFR_FSDKFace
import com.arcsoft.facerecognition.AFR_FSDKMatching
import com.arcsoft.facetracking.AFT_FSDKEngine
import com.arcsoft.facetracking.AFT_FSDKError
import com.arcsoft.facetracking.AFT_FSDKFace
import com.arcsoft.genderestimation.ASGE_FSDKFace
import com.arcsoft.genderestimation.ASGE_FSDKGender
import com.github.charleslzq.arcsofttools.kotlin.engine.*
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftRxDelegate
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSdkKey
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSetting
import com.github.charleslzq.arcsofttools.kotlin.support.ArcSoftSettingWithWebSocket
import com.github.charleslzq.faceengine.core.*
import com.github.charleslzq.faceengine.store.ReadWriteFaceStoreCacheDelegate
import com.github.charleslzq.faceengine.store.ReadWriteFaceStoreRxDelegate
import com.github.charleslzq.faceengine.support.BitmapConverter
import com.github.charleslzq.faceengine.support.toBitmap
import com.github.charleslzq.facestore.FaceFileReadWriteStore
import com.github.charleslzq.facestore.ReadWriteFaceStore
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore
import io.fotoapparat.preview.Frame
import java.util.*

/**
 * Created by charleslzq on 18-3-1.
 */
data class TrackedFace(val rect: Rect, val degree: Int)

interface FaceTracker<F> {
    fun trackFace(image: Frame): List<F>
}

interface ArcSoftFaceEngine<out D : ReadWriteFaceStore<Person, Face>>
    : FaceEngine<Frame, Person, Face, Float, D>,
        AgeDetector<Frame, DetectedAge>,
        GenderDetector<Frame, DetectedGender>,
        FaceTracker<TrackedFace>

open class ArcSoftEngineAdapterBase<S : ArcSoftSetting, out D : ReadWriteFaceStore<Person, Face>>(
        keys: ArcSoftSdkKey,
        setting: S,
        createStore: (S) -> D
) : AutoCloseable, ArcSoftFaceEngine<D> {
    final override val store = createStore(setting)
    val faceRecognitionEngine = ArcSoftFaceRecognitionEngine(keys)
    val faceDetectEngine = ArcSoftFaceDetectionEngine(keys, setting)
    val faceTrackEngine = ArcSoftFaceTrackingEngine(keys, setting)
    val ageDetectEngine = ArcSoftAgeDetectionEngine(keys, setting)
    val genderDetectEngine = ArcSoftGenderDetectionEngine(keys, setting)

    final override fun detect(image: Frame) = if (faceDetectEngine.getEngine() != null && faceRecognitionEngine.getEngine() != null) {
        val detectResult = mutableListOf<AFD_FSDKFace>()
        val detectCode = faceDetectEngine.getEngine()!!.AFD_FSDK_StillImageFaceDetection(
                image.image,
                image.size.width,
                image.size.height,
                AFD_FSDKEngine.CP_PAF_NV21,
                detectResult
        )
        if (detectCode.code == AFD_FSDKError.MOK) {
            val recognitionVersion = faceRecognitionEngine.getVersion()!!
            val pic = toBitmap(image)
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

    override fun detectAge(image: Frame) = if (ageDetectEngine.initialized()) {
        val faces = trackFace(image)
        val results = mutableListOf<ASAE_FSDKAge>()
        val errorCode = ageDetectEngine.getEngine()!!.ASAE_FSDK_AgeEstimation_Image(
                image.image,
                image.size.width,
                image.size.height,
                AFT_FSDKEngine.CP_PAF_NV21,
                faces.map { ASAE_FSDKFace(it.rect, it.degree) },
                results
        )
        if (errorCode.code == ASAE_FSDKError.MOK) {
            results.mapIndexed { index, age -> DetectedAge(FaceLocation(faces[index].rect, faces[index].degree), age.age) }
        } else {
            emptyList()
        }
    } else {
        emptyList()
    }

    override fun detectGender(image: Frame) = if (genderDetectEngine.initialized()) {
        val faces = trackFace(image)
        val results = mutableListOf<ASGE_FSDKGender>()
        val errorCode = genderDetectEngine.getEngine()!!.ASGE_FSDK_GenderEstimation_Image(
                image.image,
                image.size.width,
                image.size.height,
                AFT_FSDKEngine.CP_PAF_NV21,
                faces.map { ASGE_FSDKFace(it.rect, it.degree) },
                results
        )
        if (errorCode.code == ASAE_FSDKError.MOK) {
            results.mapIndexed { index, gender -> DetectedGender(FaceLocation(faces[index].rect, faces[index].degree), ArcSoftGender.fromCode(gender.gender)) }
        } else {
            emptyList()
        }
    } else {
        emptyList()
    }

    final override fun close() {
        faceRecognitionEngine.close()
        faceDetectEngine.close()
        faceTrackEngine.close()
        ageDetectEngine.close()
        genderDetectEngine.close()
    }

    override fun trackFace(image: Frame) = if (faceTrackEngine.initialized()) {
        val result = mutableListOf<AFT_FSDKFace>()
        val errorCode = faceTrackEngine.getEngine()!!.AFT_FSDK_FaceFeatureDetect(
                image.image,
                image.size.width,
                image.size.height,
                AFT_FSDKEngine.CP_PAF_NV21,
                result
        )
        if (errorCode.code == AFT_FSDKError.MOK) {
            result.map { TrackedFace(it.rect, it.degree) }
        } else {
            emptyList()
        }
    } else {
        emptyList()
    }
}

class ArcSoftFaceEngineService<out D : ReadWriteFaceStore<Person, Face>>(
        arcSoftFaceEngine: ArcSoftFaceEngine<D>
) : FaceEngineService<Frame, Person, Face, Float, D>(arcSoftFaceEngine),
        AgeDetector<Frame, DetectedAge> by arcSoftFaceEngine,
        GenderDetector<Frame, DetectedGender> by arcSoftFaceEngine,
        FaceTracker<TrackedFace> by arcSoftFaceEngine

class LocalArcSoftEngineService :
        FaceEngineServiceBackground<Frame, Person, Face, Float, ReadWriteFaceStore<Person, Face>>() {
    override fun createEngineService() =
            ArcSoftFaceEngineService<ReadWriteFaceStore<Person, Face>>(
                    ArcSoftRxDelegate(ArcSoftEngineAdapterBase(ArcSoftSdkKey(), ArcSoftSetting(resources)) {
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
                                                ArcSoftFaceDataType(),
                                                BitmapConverter.createGson()
                                        )
                                )
                        )
                    })
            )
}

class WebSocketArcSoftEngineService :
        FaceEngineServiceBackground<Frame, Person, Face, Float, WebSocketCompositeFaceStore<Person, Face>>() {
    override fun createEngineService() = ArcSoftFaceEngineService(
            ArcSoftRxDelegate(ArcSoftEngineAdapterBase(ArcSoftSdkKey(), ArcSoftSettingWithWebSocket(resources)) {
                WebSocketCompositeFaceStore(
                        it.webSocketUrl,
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
                                                ArcSoftFaceDataType(),
                                                BitmapConverter.createGson()
                                        )
                                )
                        )
                )
            }))

    override fun onDestroy() {
        super.onDestroy()
        engineService.engine.store.disconnect()
    }
}