package com.github.charleslzq.face.baidu

import android.graphics.Rect
import com.github.charleslzq.face.baidu.data.*
import com.github.charleslzq.faceengine.core.FaceEngineWithOptions
import com.github.charleslzq.faceengine.core.TrackedFace
import com.github.charleslzq.faceengine.support.ServiceBackground
import com.github.charleslzq.faceengine.support.ServiceConnectionBuilder
import com.github.charleslzq.faceengine.support.toEncodedBytes
import com.github.charleslzq.faceengine.view.PreviewFrame
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun PreviewFrame.toImage() = Image(Image.Type.BASE64, toEncodedBytes(this))

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

fun UserSearchResult.toUser() = UserMeta(
        groupId,
        userId,
        userInfo
)

class BaiduFaceEngine(
        baseUrl: String,
        private val retrofitBuilder: (String) -> Retrofit = {
            Retrofit.Builder()
                    .baseUrl(it.toSafeRetrofitUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .build()
        }
) : BaiduUserGroupApi, BaiduUserApi, BaiduFaceApi, BaiduImageApi,
        FaceEngineWithOptions<PreviewFrame, UserMeta, DetectedFace, DetectOptions, SearchOptions> {
    override var defaultDetectOption = DetectOptions()
    override var defaultSearchOption = SearchOptions()

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

    fun setUrlWithCallback(newUrl: String, callback: () -> Unit) {
        url = newUrl
        callback()
    }

    fun setUrlWithCallback(newUrl: String, callback: Runnable) {
        url = newUrl
        callback.run()
    }

    override fun detect(image: PreviewFrame) = runBlocking(CommonPool) {
        detect(image.toImage()).await().result?.faceList?.map { it.toTrackedFace() to it }?.toMap()
                ?: emptyMap()
    }

    override fun detectWithOption(image: PreviewFrame, option: DetectOptions) = runBlocking(CommonPool) {
        detect(image.toImage(), option.maxCount, option.source, option.fields.toTypedArray(), option.complete)
                .await().result?.faceList?.map { it.toTrackedFace() to it }?.toMap()
                ?: emptyMap()
    }

    override fun searchWithOption(image: PreviewFrame, option: SearchOptions) = runBlocking(CommonPool) {
        search(image.toImage(), option.groups.toTypedArray(), null, option.maxUser, option.quality, option.liveness)
                .await().result?.userList?.maxBy { it.score }?.toUser()
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
}

class BaiduFaceEngineServiceBackground : ServiceBackground<BaiduFaceEngine>() {
    override fun createService() = BaiduFaceEngine(BaiduSetting(resources).baseUrl)

    companion object : ServiceConnectionBuilder<BaiduFaceEngine>
}