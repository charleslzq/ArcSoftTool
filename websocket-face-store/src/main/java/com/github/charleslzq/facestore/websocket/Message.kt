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
    FACE_ID("faceId")
}

enum class ClientMessagePayloadTypes {
    REFRESH,
    PERSON,
    FACE,
    FACE_DATA,
    PERSON_DELETE,
    FACE_DELETE,
    FACE_CLEAR
}

enum class ServerMessagePayloadTypes {
    PERSON,
    FACE,
    PERSON_DELETE,
    FACE_DELETE,
    FACE_CLEAR
}