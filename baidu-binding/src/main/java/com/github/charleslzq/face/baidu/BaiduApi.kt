package com.github.charleslzq.face.baidu

import com.github.charleslzq.face.baidu.data.*
import kotlinx.coroutines.experimental.Deferred
import retrofit2.http.*

interface BaiduUserGroupApi {
    @GET("groups")
    fun list(
            @Query("start") start: Int = 0,
            @Query("length") length: Int = 100
    ): Deferred<BaiduResponse<GroupIdList>>

    @POST("groups")
    fun add(@Body groupId: String): Deferred<BaiduResponse<*>>

    @DELETE("groups/{id}")
    fun delete(@Path("id") id: String): Deferred<BaiduResponse<*>>

    @PUT("groups/{id}")
    fun copy(
            @Path("id") id: String,
            @Query("srcGroupId") srcGroupId: String,
            @Query("userId") userId: String
    )
}

interface BaiduUserApi {
    @GET("groups/{groupId}/users")
    fun list(
            @Path("groupId") groupId: String,
            @Query("start") start: Int = 0,
            @Query("length") length: Int = 100
    ): Deferred<BaiduResponse<UserIdList>>

    @POST("groups/{groupId}/users")
    fun add(
            @Path("groupId") groupId: String,
            @Body image: RegisterImage,
            @Query("quality") quality: QualityControl = QualityControl.NONE,
            @Query("liveness") liveness: LivenessControl = LivenessControl.NONE
    ): Deferred<BaiduResponse<FaceOperationResult>>

    @PUT("groups/{groupId}/users/{id}")
    fun update(
            @Path("groupId") groupId: String,
            @Path("id") id: String,
            @Body image: UpdateImage,
            @Query("quality") quality: QualityControl = QualityControl.NONE,
            @Query("liveness") liveness: LivenessControl = LivenessControl.NONE
    ): Deferred<BaiduResponse<FaceOperationResult>>

    @GET("groups/{groupId}/users/{id}")
    fun get(
            @Path("groupId") groupId: String,
            @Path("id") id: String
    ): Deferred<BaiduResponse<UserQueryResult>>

    @DELETE("groups/{groupId}/users/{id}")
    fun delete(
            @Path("groupId") groupId: String,
            @Path("id") id: String
    ): Deferred<BaiduResponse<*>>

    data class RegisterImage(
            val image: Image = Image(),
            val userId: String = "",
            val userInfo: String? = null
    )

    data class UpdateImage(
            val image: Image = Image(),
            val userInfo: String? = null
    )
}

interface BaiduFaceApi {
    @GET("groups/{groupId}/users/{userId}/faces")
    fun list(
            @Path("groupId") groupId: String,
            @Path("userId") userId: String
    ): Deferred<BaiduResponse<FaceListResult>>

    @DELETE("groups/{groupId}/users/{userId}/faces/{faceToken}")
    fun delete(
            @Path("groupId") groupId: String,
            @Path("userId") userId: String,
            @Path("faceToken") faceToken: String
    ): Deferred<BaiduResponse<*>>
}

interface BaiduImageApi {
    @POST("images/detect")
    fun detect(
            @Body image: Image,
            @Query("maxCount") maxCount: Int = 1,
            @Query("source") source: FaceSource = FaceSource.LIVE,
            @Query("fields") fields: Array<FaceField> = emptyArray(),
            @Query("complete") complete: Boolean = false
    ): Deferred<BaiduResponse<DetectResult>>

    @POST("images/search")
    fun search(
            @Body image: Image,
            @Query("groups") groups: Array<String>,
            @Query("userId") userId: String? = null,
            @Query("maxUser") maxUser: Int = 1,
            @Query("quality") quality: QualityControl = QualityControl.NONE,
            @Query("liveness") liveness: LivenessControl = LivenessControl.NONE
    ): Deferred<BaiduResponse<SearchResult>>

    @POST("images/match")
    fun match(
            @Body images: Array<MatchReq>
    ): Deferred<BaiduResponse<MatchResult>>

    @POST("images/verify")
    fun verify(
            @Body images: Array<FaceVerifyReq>
    ): Deferred<BaiduResponse<VerifyResult>>
}