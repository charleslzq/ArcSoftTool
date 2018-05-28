package com.github.charleslzq.face.baidu

import android.graphics.Rect
import com.github.charleslzq.faceengine.core.FaceEngine
import com.github.charleslzq.faceengine.core.FaceEngineService
import com.github.charleslzq.faceengine.core.FaceEngineServiceBackground
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.toEncodedBytes
import com.github.charleslzq.faceengine.view.CameraPreview
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

fun CameraPreview.PreviewFrame.toImage() = Image(Image.Type.BASE64, toEncodedBytes(this))

fun DetectedFace.leftInt() = location.left.toInt()

fun DetectedFace.topInt() = location.top.toInt()

fun DetectedFace.rightInt() = (location.left + location.width).toInt()

fun DetectedFace.bottomInt() = (location.top + location.height).toInt()

fun DetectedFace.rect() = Rect(
        leftInt(),
        topInt(),
        rightInt(),
        bottomInt()
)

fun DetectedFace.toTrackedFace() = TrackedFace(rect(), location.rotation)

fun UserSearchResult.toUser() = BaiduFaceEngine.User(
        groupId,
        userId,
        userInfo
)

class BaiduFaceEngine(
        baseUrl: String,
        private var retrofit: Retrofit = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build(),
        private var groupApi: BaiduUserGroupApi = retrofit.create(BaiduUserGroupApi::class.java),
        private var userApi: BaiduUserApi = retrofit.create(BaiduUserApi::class.java),
        private var faceApi: BaiduFaceApi = retrofit.create(BaiduFaceApi::class.java),
        private var imageApi: BaiduImageApi = retrofit.create(BaiduImageApi::class.java)
) : BaiduUserGroupApi by groupApi, BaiduUserApi by userApi, BaiduFaceApi by faceApi, BaiduImageApi by imageApi,
        FaceEngine<CameraPreview.PreviewFrame, BaiduFaceEngine.User, DetectedFace> {
    var url = baseUrl
        set(value) {
            if (value != field) {
                retrofit = Retrofit.Builder().baseUrl(value).addConverterFactory(GsonConverterFactory.create()).build()
                groupApi = retrofit.create(BaiduUserGroupApi::class.java)
                userApi = retrofit.create(BaiduUserApi::class.java)
                faceApi = retrofit.create(BaiduFaceApi::class.java)
                imageApi = retrofit.create(BaiduImageApi::class.java)
                field = value
            }
        }

    override fun detect(image: CameraPreview.PreviewFrame) = runBlocking(CommonPool) {
        detect(image.toImage()).awaitResult().let {
            when (it) {
                is Result.Ok -> it.value.result?.faceList?.map { it.toTrackedFace() to it }?.toMap()
                        ?: emptyMap()
                else -> emptyMap()
            }
        }
    }

    override fun search(image: CameraPreview.PreviewFrame) = runBlocking(CommonPool) {
        list().awaitResult().let {
            when (it) {
                is Result.Ok -> it.value.result?.groupIdList?.toTypedArray()?.let {
                    search(image.toImage(), it).awaitResult().let {
                        when (it) {
                            is Result.Ok -> it.value.result?.userList?.maxBy { it.score }?.toUser()
                            else -> null
                        }
                    }
                }
                else -> null
            }
        }
    }


    data class User(
            val groupId: String,
            val userId: String,
            val userInfo: String? = null
    )
}

class BaiduFaceEngineService(
        engine: BaiduFaceEngine
) : FaceEngineService<CameraPreview.PreviewFrame, BaiduFaceEngine.User, DetectedFace, BaiduFaceEngine>(engine),
        BaiduUserGroupApi by engine,
        BaiduUserApi by engine,
        BaiduFaceApi by engine,
        BaiduImageApi by engine {
    var url: String
        get() = engine.url
        set(value) {
            engine.url = value
        }
}

class BaiduFaceEngineServiceBackground : FaceEngineServiceBackground<CameraPreview.PreviewFrame, BaiduFaceEngine.User, DetectedFace, BaiduFaceEngine>() {
    override fun createEngineService() = BaiduFaceEngineService(BaiduFaceEngine(BaiduSetting(resources).baseUrl))
}