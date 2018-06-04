package com.github.charleslzq.face.baidu

import com.github.charleslzq.face.baidu.data.*
import kotlinx.coroutines.experimental.Deferred
import retrofit2.http.*

interface BaiduUserGroupApi {
    @GET("groups")
    fun listGroup(
            @Query("open") start: Int = 0,
            @Query("length") length: Int = 100
    ): Deferred<BaiduResponse<GroupIdList>>

    @POST("groups")
    fun addGroup(@Body groupId: String): Deferred<BaiduResponse<Any>>

    @DELETE("groups/{id}")
    fun deleteGroup(@Path("id") id: String): Deferred<BaiduResponse<Any>>

    @PUT("groups/{id}")
    fun copyUser(
            @Path("id") id: String,
            @Query("srcGroupId") srcGroupId: String,
            @Query("userId") userId: String
    ): Deferred<BaiduResponse<Any>>
}

interface BaiduUserApi {
    @GET("groups/{groupId}/users")
    fun listUser(
            @Path("groupId") groupId: String,
            @Query("open") start: Int = 0,
            @Query("length") length: Int = 100
    ): Deferred<BaiduResponse<UserIdList>>

    @POST("groups/{groupId}/users")
    fun addUser(
            @Path("groupId") groupId: String,
            @Body image: RegisterImage,
            @Query("quality") quality: QualityControl = QualityControl.NONE,
            @Query("liveness") liveness: LivenessControl = LivenessControl.NONE
    ): Deferred<BaiduResponse<FaceOperationResult>>

    @PUT("groups/{groupId}/users/{id}")
    fun updateUser(
            @Path("groupId") groupId: String,
            @Path("id") id: String,
            @Body image: UpdateImage,
            @Query("quality") quality: QualityControl = QualityControl.NONE,
            @Query("liveness") liveness: LivenessControl = LivenessControl.NONE
    ): Deferred<BaiduResponse<FaceOperationResult>>

    @GET("groups/{groupId}/users/{id}")
    fun queryUser(
            @Path("groupId") groupId: String,
            @Path("id") id: String
    ): Deferred<BaiduResponse<UserQueryResult>>

    @DELETE("groups/{groupId}/users/{id}")
    fun deleteUser(
            @Path("groupId") groupId: String,
            @Path("id") id: String
    ): Deferred<BaiduResponse<Any>>

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
    fun listFace(
            @Path("groupId") groupId: String,
            @Path("userId") userId: String
    ): Deferred<BaiduResponse<FaceListResult>>

    @DELETE("groups/{groupId}/users/{userId}/faces/{faceToken}")
    fun deleteFace(
            @Path("groupId") groupId: String,
            @Path("userId") userId: String,
            @Path("faceToken") faceToken: String
    ): Deferred<BaiduResponse<Any>>
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