package com.github.charleslzq.face.baidu

import android.graphics.Rect
import com.github.charleslzq.face.baidu.data.*
import com.github.charleslzq.faceengine.core.FaceEngine
import com.github.charleslzq.faceengine.core.FaceEngineService
import com.github.charleslzq.faceengine.core.FaceEngineServiceBackground
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.toEncodedBytes
import com.github.charleslzq.faceengine.view.CameraPreview
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
        val retrofitBuilder: (String) -> Retrofit = {
            Retrofit.Builder()
                    .baseUrl(it.toSafeRetrofitUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .build()
        }
) : BaiduUserGroupApi, BaiduUserApi, BaiduFaceApi, BaiduImageApi,
        FaceEngine<CameraPreview.PreviewFrame, BaiduFaceEngine.User, DetectedFace> {
    var url = baseUrl
        set(value) {
            if (value != field) {
                retrofit = retrofitBuilder(value)
                groupApi = retrofit.create(BaiduUserGroupApi::class.java)
                userApi = retrofit.create(BaiduUserApi::class.java)
                faceApi = retrofit.create(BaiduFaceApi::class.java)
                imageApi = retrofit.create(BaiduImageApi::class.java)
                field = value
            }
        }
    private var retrofit: Retrofit = retrofitBuilder(baseUrl)
    private var groupApi: BaiduUserGroupApi = retrofit.create(BaiduUserGroupApi::class.java)
    private var userApi: BaiduUserApi = retrofit.create(BaiduUserApi::class.java)
    private var faceApi: BaiduFaceApi = retrofit.create(BaiduFaceApi::class.java)
    private var imageApi: BaiduImageApi = retrofit.create(BaiduImageApi::class.java)

    var defaultSearchGroups = mutableListOf<String>()

    fun <T> blockingGet(job: Deferred<T>) = runBlocking { job.await() }

    override fun detect(image: CameraPreview.PreviewFrame) = runBlocking(CommonPool) {
        detect(image.toImage()).await().result?.faceList?.map { it.toTrackedFace() to it }?.toMap()
                ?: emptyMap()
    }

    override fun search(image: CameraPreview.PreviewFrame) = runBlocking(CommonPool) {
        search(image.toImage(), defaultSearchGroups.toTypedArray()).await().result?.userList?.maxBy { it.score }?.toUser()
    }

    override fun listGroup(start: Int, length: Int) = groupApi.listGroup(start, length)

    override fun addGroup(groupId: String) = groupApi.addGroup(groupId)

    override fun deleteGroup(id: String) = groupApi.deleteGroup(id)

    override fun copyUser(id: String, srcGroupId: String, userId: String) = groupApi.copyUser(id, srcGroupId, userId)

    override fun listUser(groupId: String, start: Int, length: Int) = userApi.listUser(groupId, start, length)

    override fun addUser(groupId: String, image: BaiduUserApi.RegisterImage, quality: QualityControl, liveness: LivenessControl) = userApi.addUser(groupId, image, quality, liveness)

    override fun updateUser(groupId: String, id: String, image: BaiduUserApi.UpdateImage, quality: QualityControl, liveness: LivenessControl) = userApi.updateUser(groupId, id, image, quality, liveness)

    override fun queryUser(groupId: String, id: String) = userApi.queryUser(groupId, id)

    override fun deleteUser(groupId: String, id: String) = userApi.deleteUser(groupId, id)

    override fun listFace(groupId: String, userId: String) = faceApi.listFace(groupId, userId)

    override fun deleteFace(groupId: String, userId: String, faceToken: String) = faceApi.deleteFace(groupId, userId, faceToken)

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