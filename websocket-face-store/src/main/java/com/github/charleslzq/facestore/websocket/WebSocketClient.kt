package com.github.charleslzq.facestore.websocket

import android.util.Log
import com.github.charleslzq.faceengine.support.runOnIo
import com.koushikdutta.async.http.AsyncHttpClient
import com.koushikdutta.async.http.WebSocket

/**
 * Created by charleslzq on 18-3-19.
 */
class WebSocketClient(
        var url: String,
        private val onMessage: (String) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val logTag = javaClass.name

    fun isOpen() = webSocket?.isOpen ?: false

    fun connect() = runOnIo {
        AsyncHttpClient.getDefaultInstance().websocket(url, "web-socket", ::onComplete)
    }

    fun send(message: String) = runOnIo {
        webSocket?.send(message)
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