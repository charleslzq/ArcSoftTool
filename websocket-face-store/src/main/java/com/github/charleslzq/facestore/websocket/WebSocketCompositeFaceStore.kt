package com.github.charleslzq.facestore.websocket

import com.github.charleslzq.faceengine.support.BitmapConverter
import com.github.charleslzq.facestore.CompositeReadWriteFaceStore
import com.github.charleslzq.facestore.FaceData
import com.github.charleslzq.facestore.Meta
import com.github.charleslzq.facestore.ReadWriteFaceStore
import com.google.gson.Gson

/**
 * Created by charleslzq on 18-3-19.
 */
class WebSocketCompositeFaceStore<P : Meta, F : Meta>
@JvmOverloads
constructor(
        url: String,
        private val localStore: ReadWriteFaceStore<P, F>,
        private val gson: Gson = BitmapConverter.createGson()
) : CompositeReadWriteFaceStore<P, F>(localStore) {
    private val client = WebSocketClient(url, ::onMessage)

    var url = url
        set(value) {
            field = value
            client.url = value
            client.end()
        }

    fun refresh() {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.REFRESH.name
        ).toMutableMap()
        connect()
        client.send(gson.toJson(Message(headers, "refresh")))
    }

    override fun savePerson(person: P) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.PERSON.name,
                MessageHeaders.PERSON_ID.value to person.id
        ).toMutableMap()
        connect()
        client.send(gson.toJson(Message(headers, person)))
    }

    override fun saveFace(personId: String, face: F) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.FACE.name,
                MessageHeaders.PERSON_ID.value to personId,
                MessageHeaders.FACE_ID.value to face.id
        ).toMutableMap()
        connect()
        client.send(gson.toJson(Message(headers, face)))
    }

    override fun saveFaceData(faceData: FaceData<P, F>) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.FACE_DATA.name,
                MessageHeaders.PERSON_ID.value to faceData.person.id
        ).toMutableMap()
        connect()
        client.send(gson.toJson(Message(headers, faceData)))
    }

    override fun deleteFaceData(personId: String) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.PERSON_DELETE.name,
                MessageHeaders.PERSON_ID.value to personId
        ).toMutableMap()
        connect()
        client.send(gson.toJson(Message(headers, personId)))
    }

    override fun clearFace(personId: String) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.FACE_CLEAR.name,
                MessageHeaders.PERSON_ID.value to personId
        ).toMutableMap()
        connect()
        client.send(gson.toJson(Message(headers, personId)))
    }

    override fun deleteFace(personId: String, faceId: String) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.FACE_DELETE.name,
                MessageHeaders.PERSON_ID.value to personId,
                MessageHeaders.FACE_ID.value to faceId
        ).toMutableMap()
        connect()
        client.send(gson.toJson(Message(headers, faceId)))
    }

    private fun connect() {
        if (!client.isOpen()) {
            client.connect()
        }
    }

    private fun onMessage(message: String) {
        val rawMessage = gson.fromJson<Message<Any>>(
                message,
                Message::class.java
        )
        rawMessage.headers[MessageHeaders.TYPE_HEADER.value]?.let {
            when (ServerMessagePayloadTypes.valueOf(it)) {
                ServerMessagePayloadTypes.PERSON -> localStore.savePerson(gson.fromJson(gson.toJson(rawMessage.payload), dataType.personClass))
                ServerMessagePayloadTypes.FACE -> localStore.saveFace(checkAndGet(rawMessage.headers, MessageHeaders.PERSON_ID), gson.fromJson(gson.toJson(rawMessage.payload), dataType.faceClass))
                ServerMessagePayloadTypes.PERSON_DELETE -> localStore.deleteFaceData(checkAndGet(rawMessage.headers, MessageHeaders.PERSON_ID))
                ServerMessagePayloadTypes.FACE_DELETE -> localStore.deleteFace(
                        checkAndGet(rawMessage.headers, MessageHeaders.PERSON_ID),
                        checkAndGet(rawMessage.headers, MessageHeaders.FACE_ID)
                )
                ServerMessagePayloadTypes.FACE_CLEAR -> localStore.clearFace(
                        checkAndGet(rawMessage.headers, MessageHeaders.PERSON_ID)
                )
            }
        }
    }

    private fun checkAndGet(headers: Map<String, String>, key: MessageHeaders) = headers[key.value]
            ?: throw IllegalArgumentException("Required header ${key.value} not found")
}