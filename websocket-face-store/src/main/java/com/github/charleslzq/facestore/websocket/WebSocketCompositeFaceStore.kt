package com.github.charleslzq.facestore.websocket

import com.github.charleslzq.faceengine.support.BitmapConverter
import com.github.charleslzq.facestore.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by charleslzq on 18-3-19.
 */
interface WebSocketFaceStoreInstance : WebSocketInstance {
    fun refresh()
    fun <T> send(message: Message<T>)
}

class WebSocketFaceStoreBroker<P : Meta, F : Meta>
@JvmOverloads
constructor(
        url: String,
        val localStore: ReadWriteFaceStore<P, F>,
        private val gson: Gson = BitmapConverter.createGson(),
        private val allowSend: Boolean = false
) : WebSocketFaceStoreInstance {
    private val idListMessageType = object : TypeToken<Message<List<String>>>() {}
    private val client = WebSocketClient(url, ::onMessage)
    override var url: String
        get() = client.url
        set(value) {
            client.url = value
            disconnect()
        }

    override fun connect() {
        if (!isOpen()) {
            client.connect()
        }
    }

    override fun disconnect() = client.disconnect()

    override fun isOpen() = client.isOpen()

    override fun send(message: String) {
        if (allowSend) {
            connect()
            client.send(message)
        } else {
            throw UnsupportedOperationException("Not Allowed To Send Message")
        }
    }

    override fun <T> send(message: Message<T>) {
        send(gson.toJson(message))
    }

    override fun refresh() {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.REFRESH.name
        ).toMutableMap()
        client.send(gson.toJson(Message(headers, "refresh")))
    }

    private fun onMessage(message: String) {
        val rawMessage = gson.fromJson<Message<Any>>(
                message,
                Message::class.java
        )
        rawMessage.headers[MessageHeaders.TYPE_HEADER.value]?.let {
            when (ServerMessagePayloadTypes.valueOf(it)) {
                ServerMessagePayloadTypes.PERSON -> localStore.savePerson(gson.fromJson(gson.toJson(rawMessage.payload), localStore.dataType.personClass))
                ServerMessagePayloadTypes.PERSON_ID_LIST -> {
                    val idListMessage = gson.fromJson<Message<List<String>>>(message, idListMessageType.type)
                    localStore.getPersonIds().minus(idListMessage.payload).forEach {
                        localStore.deleteFaceData(it)
                    }
                }
                ServerMessagePayloadTypes.FACE -> localStore.saveFace(
                        checkAndGet(rawMessage.headers, MessageHeaders.PERSON_ID),
                        gson.fromJson(gson.toJson(rawMessage.payload), localStore.dataType.faceClass)
                )
                ServerMessagePayloadTypes.FACE_ID_LIST -> {
                    val idListMessage = gson.fromJson<Message<List<String>>>(message, idListMessageType.type)
                    val personId = checkAndGet(idListMessage.headers, MessageHeaders.PERSON_ID)
                    localStore.getFaceIdList(personId).minus(idListMessage.payload).forEach {
                        localStore.deleteFace(personId, it)
                    }
                }
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

class WebSocketReadOnlyFaceStore<P : Meta, F : Meta>
@JvmOverloads
constructor(
        url: String,
        localStore: ReadWriteFaceStore<P, F>,
        gson: Gson = BitmapConverter.createGson()
) : ReadOnlyFaceStore<P, F> by localStore, WebSocketFaceStoreInstance by WebSocketFaceStoreBroker<P, F>(url, localStore, gson, false)

class WebSocketCompositeFaceStore<P : Meta, F : Meta>
@JvmOverloads
constructor(
        url: String,
        localStore: ReadWriteFaceStore<P, F>,
        gson: Gson = BitmapConverter.createGson()
) : CompositeReadWriteFaceStore<P, F>(localStore), WebSocketFaceStoreInstance by WebSocketFaceStoreBroker<P, F>(url, localStore, gson, true) {

    override fun savePerson(person: P) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.PERSON.name,
                MessageHeaders.PERSON_ID.value to person.id
        ).toMutableMap()
        send(Message(headers, person))
    }

    override fun saveFace(personId: String, face: F) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.FACE.name,
                MessageHeaders.PERSON_ID.value to personId,
                MessageHeaders.FACE_ID.value to face.id
        ).toMutableMap()
        send(Message(headers, face))
    }

    override fun saveFaceData(faceData: FaceData<P, F>) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.FACE_DATA.name,
                MessageHeaders.PERSON_ID.value to faceData.person.id
        ).toMutableMap()
        send(Message(headers, faceData))
    }

    override fun deleteFaceData(personId: String) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.PERSON_DELETE.name,
                MessageHeaders.PERSON_ID.value to personId
        ).toMutableMap()
        send(Message(headers, personId))
    }

    override fun clearFace(personId: String) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.FACE_CLEAR.name,
                MessageHeaders.PERSON_ID.value to personId
        ).toMutableMap()
        send(Message(headers, personId))
    }

    override fun deleteFace(personId: String, faceId: String) {
        val headers = mapOf(
                MessageHeaders.TYPE_HEADER.value to ClientMessagePayloadTypes.FACE_DELETE.name,
                MessageHeaders.PERSON_ID.value to personId,
                MessageHeaders.FACE_ID.value to faceId
        ).toMutableMap()
        send(Message(headers, faceId))
    }
}