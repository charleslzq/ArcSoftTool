package com.github.charleslzq.face.baidu

import android.graphics.Rect
import com.github.charleslzq.face.baidu.data.*
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
        baseUrl: String
) : BaiduUserGroupApi, BaiduUserApi, BaiduFaceApi, BaiduImageApi,
        FaceEngine<CameraPreview.PreviewFrame, BaiduFaceEngine.User, DetectedFace> {
    var url = baseUrl
        set(value) {
            if (value != field) {
                retrofit = Retrofit.Builder()
                        .baseUrl(value.toSafeRetrofitUrl())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                groupApi = retrofit.create(BaiduUserGroupApi::class.java)
                userApi = retrofit.create(BaiduUserApi::class.java)
                faceApi = retrofit.create(BaiduFaceApi::class.java)
                imageApi = retrofit.create(BaiduImageApi::class.java)
                field = value
            }
        }
    private var retrofit: Retrofit = Retrofit.Builder().baseUrl(baseUrl.toSafeRetrofitUrl()).addConverterFactory(GsonConverterFactory.create()).build()
    private var groupApi: BaiduUserGroupApi = retrofit.create(BaiduUserGroupApi::class.java)
    private var userApi: BaiduUserApi = retrofit.create(BaiduUserApi::class.java)
    private var faceApi: BaiduFaceApi = retrofit.create(BaiduFaceApi::class.java)
    private var imageApi: BaiduImageApi = retrofit.create(BaiduImageApi::class.java)

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

    override fun list(start: Int, length: Int) = groupApi.list(start, length)

    override fun add(groupId: String) = groupApi.add(groupId)

    override fun delete(id: String) = groupApi.delete(id)

    override fun copy(id: String, srcGroupId: String, userId: String) = groupApi.copy(id, srcGroupId, userId)

    override fun list(groupId: String, start: Int, length: Int) = userApi.list(groupId, start, length)

    override fun add(groupId: String, image: BaiduUserApi.RegisterImage, quality: QualityControl, liveness: LivenessControl) = userApi.add(groupId, image, quality, liveness)

    override fun update(groupId: String, id: String, image: BaiduUserApi.UpdateImage, quality: QualityControl, liveness: LivenessControl) = userApi.update(groupId, id, image, quality, liveness)

    override fun get(groupId: String, id: String) = userApi.get(groupId, id)

    override fun delete(groupId: String, id: String) = userApi.delete(groupId, id)

    override fun list(groupId: String, userId: String) = faceApi.list(groupId, userId)

    override fun delete(groupId: String, userId: String, faceToken: String) = faceApi.delete(groupId, userId, faceToken)

    override fun detect(image: Image, maxCount: Int, source: FaceSource, fields: Array<FaceField>, complete: Boolean) = imageApi.detect(image, maxCount, source, fields, complete)

    override fun search(image: Image, groups: Array<String>, userId: String?, maxUser: Int, quality: QualityControl, liveness: LivenessControl) = imageApi.search(image, groups, userId, maxUser, quality, liveness)

    override fun match(images: Array<MatchReq>) = imageApi.match(images)

    override fun verify(images: Array<FaceVerifyReq>) = imageApi.verify(images)


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

    fun setUrlWithCallback(newUrl: String, callback: () -> Unit) {
        url = newUrl
        callback()
    }
}

class BaiduFaceEngineServiceBackground : FaceEngineServiceBackground<CameraPreview.PreviewFrame, BaiduFaceEngine.User, DetectedFace, BaiduFaceEngine>() {
    override fun createEngineService() = BaiduFaceEngineService(BaiduFaceEngine(BaiduSetting(resources).baseUrl))

    companion object : ConnectionBuilder<CameraPreview.PreviewFrame, BaiduFaceEngine.User, DetectedFace, BaiduFaceEngine, BaiduFaceEngineService>
}