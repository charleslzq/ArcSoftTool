package com.github.charleslzq.facestore.websocket

import android.util.Log
import com.github.charleslzq.faceengine.support.runOnIo
import com.koushikdutta.async.http.AsyncHttpClient
import com.koushikdutta.async.http.WebSocket

/**
 * Created by charleslzq on 18-3-19.
 */
interface WebSocketInstance {
    var url: String
    fun isOpen(): Boolean
    fun connect()
    fun disconnect()
    fun send(message: String)
}

class WebSocketClient(
        override var url: String,
        private val onMessage: (String) -> Unit
) : WebSocketInstance {
    private var webSocket: WebSocket? = null
    private val logTag = javaClass.name

    override fun isOpen() = webSocket?.isOpen ?: false

    override fun connect() {
        runOnIo {
            AsyncHttpClient.getDefaultInstance().websocket(url, "web-socket", ::onComplete)
        }
    }

    override fun disconnect() {
        if (isOpen()) {
            webSocket?.end()
        }
    }

    override fun send(message: String) {
        runOnIo {
            webSocket?.send(message)
        }
    }

    private fun onComplete(ex: Exception?, webSocket: WebSocket?) {
        when (ex == null && webSocket != null) {
            true -> {
                webSocket!!.setStringCallback {
                    if (it != null && it != heartBeat) {
                        onMessage(it)
                    }
                }
                this.webSocket = webSocket
            }
            false -> {
                Log.e(logTag, "Error when connecting $url", ex)
            }
        }
    }

    companion object {
        private const val heartBeat = "@heart"
    }
}