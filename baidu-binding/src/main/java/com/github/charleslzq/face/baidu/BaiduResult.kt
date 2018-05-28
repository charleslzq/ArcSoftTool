package com.github.charleslzq.face.baidu


data class BaiduResponse<D>(
        val result: D?,
        val logId: String,
        val errorCode: String,
        val errorMsg: String,
        val timestamp: Long
)

data class DetectResult(
        val count: Int,
        val faceList: List<DetectedFace>
)

data class SearchResult(
        val faceToken: String,
        val userList: List<UserSearchResult>
)

data class UserSearchResult(
        val groupId: String,
        val userId: String,
        val userInfo: String,
        val score: Float
)

data class FaceOperationResult(
        val faceToken: String,
        val location: FaceLocation
)

data class UserQueryResult(
        val userList: List<QueriedUser>
)

data class FaceListResult(
        val faceList: List<FaceListItem>
)

data class UserIdList(
        val userIdList: List<String>
)

data class GroupIdList(
        val groupIdList: List<String>
)

data class MatchResult(
        val score: Float,
        val faceList: List<FaceToken>
)

data class VerifyResult(
        val liveness: Float,
        val thresholds: VerifyThreshold,
        val faceList: List<DetectedFace>
)