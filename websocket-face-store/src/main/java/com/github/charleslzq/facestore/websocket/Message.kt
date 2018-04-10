package com.github.charleslzq.facestore.websocket

/**
 * Created by charleslzq on 18-3-19.
 */
data class Message<T>(
        var headers: MutableMap<String, String> = emptyMap<String, String>().toMutableMap(),
        var payload: T
)

enum class MessageHeaders(val value: String) {
    TYPE_HEADER("TYPE_HEADER"),
    PERSON_ID("personId"),
    FACE_ID("faceId"),
    TIMESTAMP("timestamp"),
    INDEX("index"),
    SIZE("size"),
    TOKEN("token")
}

enum class ClientMessagePayloadTypes {
    REFRESH,
    PERSON,
    FACE,
    PERSON_DELETE,
    FACE_DELETE
}

enum class ServerMessagePayloadTypes {
    PERSON,
    PERSON_ID_LIST,
    FACE,
    FACE_ID_LIST,
    PERSON_DELETE,
    FACE_DELETE,
    CONFIRM
}