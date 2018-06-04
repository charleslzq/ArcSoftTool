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
import com.github.charleslzq.faceengine.support.ServiceBackground
import com.github.charleslzq.faceengine.support.ServiceConnectionBuilder
import com.github.charleslzq.faceengine.support.toBitmap
import com.github.charleslzq.faceengine.view.PreviewFrame
import com.github.charleslzq.facestore.FaceFileReadWriteStore
import com.github.charleslzq.facestore.ReadWriteFaceStore
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore
import java.util.*

/**
 * Created by charleslzq on 18-3-1.
 */
interface ArcSoftFaceOfflineEngine<D : ReadWriteFaceStore<Person, Face>>
    : FaceOfflineEngine<PreviewFrame, Person, Face, Float, D>,
        AgeDetector<PreviewFrame, DetectedAge>,
        GenderDetector<PreviewFrame, DetectedGender>,
        FaceTracker<PreviewFrame>

open class ArcSoftOfflineEngineAdapterBase<S : ArcSoftSetting, D : ReadWriteFaceStore<Person, Face>>(
        keys: ArcSoftSdkKey,
        setting: S,
        createStore: (S) -> D
) : AutoCloseable, ArcSoftFaceOfflineEngine<D> {
    final override var threshold: Float = 0.5f
    final override val store = createStore(setting)
    val faceRecognitionEngine = ArcSoftFaceRecognitionEngine(keys)
    val faceDetectEngine = ArcSoftFaceDetectionEngine(keys, setting)
    val faceTrackEngine = ArcSoftFaceTrackingEngine(keys, setting)
    val ageDetectEngine = ArcSoftAgeDetectionEngine(keys, setting)
    val genderDetectEngine = ArcSoftGenderDetectionEngine(keys, setting)

    final override fun detect(image: PreviewFrame) = if (faceDetectEngine.getEngine() != null && faceRecognitionEngine.getEngine() != null) {
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
                    TrackedFace(it.rect, it.degree) to Face(UUID.randomUUID().toString(), pic, extractResult, recognitionVersion)
                } else {
                    null
                }
            }.toMap()
        } else {
            emptyMap()
        }
    } else {
        emptyMap()
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

    override fun detectAge(image: PreviewFrame) = if (ageDetectEngine.initialized()) {
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

    override fun detectGender(image: PreviewFrame) = if (genderDetectEngine.initialized()) {
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

    override fun trackFace(image: PreviewFrame) = if (faceTrackEngine.initialized()) {
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

class ArcSoftFaceEngineService<D : ReadWriteFaceStore<Person, Face>>(
        arcSoftFaceEngine: ArcSoftFaceOfflineEngine<D>
) : FaceEngineService<PreviewFrame, Person, Face, ArcSoftFaceOfflineEngine<D>>(arcSoftFaceEngine),
        AgeDetector<PreviewFrame, DetectedAge> by arcSoftFaceEngine,
        GenderDetector<PreviewFrame, DetectedGender> by arcSoftFaceEngine,
        FaceTracker<PreviewFrame> by arcSoftFaceEngine

class LocalArcSoftEngineService :
        FaceEngineServiceBackground<PreviewFrame, Person, Face, ArcSoftFaceOfflineEngine<ReadWriteFaceStore<Person, Face>>>() {
    override fun createEngineService() =
            ArcSoftFaceEngineService(
                    ArcSoftRxDelegate(ArcSoftOfflineEngineAdapterBase(ArcSoftSdkKey.read(applicationContext), ArcSoftSetting(resources)) {
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
                                                Person::class.java,
                                                Face::class.java,
                                                BitmapConverter.createGson()
                                        )
                                )
                        )
                    }) as ArcSoftFaceOfflineEngine<ReadWriteFaceStore<Person, Face>>
            )
}

class LocalArcSoftService : ServiceBackground<ArcSoftFaceOfflineEngine<ReadWriteFaceStore<Person, Face>>>() {
    override fun createService() = ArcSoftRxDelegate(ArcSoftOfflineEngineAdapterBase(ArcSoftSdkKey.read(applicationContext), ArcSoftSetting(resources)) {
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
                                Person::class.java,
                                Face::class.java,
                                BitmapConverter.createGson()
                        )
                )
        )
    }) as ArcSoftFaceOfflineEngine<ReadWriteFaceStore<Person, Face>>

    companion object : ServiceConnectionBuilder<ArcSoftFaceOfflineEngine<ReadWriteFaceStore<Person, Face>>>
}

class WebSocketArcSoftEngineService :
        FaceEngineServiceBackground<PreviewFrame, Person, Face, ArcSoftFaceOfflineEngine<WebSocketCompositeFaceStore<Person, Face>>>() {
    override fun createEngineService() = ArcSoftFaceEngineService(
            ArcSoftRxDelegate(ArcSoftOfflineEngineAdapterBase(ArcSoftSdkKey.read(applicationContext), ArcSoftSettingWithWebSocket(resources)) {
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
                                                Person::class.java,
                                                Face::class.java,
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

class WebSocketArcSoftService : ServiceBackground<ArcSoftFaceOfflineEngine<WebSocketCompositeFaceStore<Person, Face>>>() {
    override fun createService() = ArcSoftOfflineEngineAdapterBase(ArcSoftSdkKey.read(applicationContext), ArcSoftSettingWithWebSocket(resources)) {
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
                                        Person::class.java,
                                        Face::class.java,
                                        BitmapConverter.createGson()
                                )
                        )
                )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        binder.instance.store.disconnect()
    }

    companion object : ServiceConnectionBuilder<ArcSoftFaceOfflineEngine<WebSocketCompositeFaceStore<Person, Face>>>
}