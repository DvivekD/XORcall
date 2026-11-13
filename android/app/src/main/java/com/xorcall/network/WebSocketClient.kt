package com.xorcall.network

import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface TelemetryListener {
    fun onTelemetryReceived(json: JSONObject)
    fun onConnectionStateChanged(isConnected: Boolean)
}

class TelemetryWebSocketClient(
    private val serverUrl: String,
    private val listener: TelemetryListener
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null

    fun connect(sessionId: String) {
        val request = Request.Builder()
            .url("$serverUrl/ws/call-stream/$sessionId")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener.onConnectionStateChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    listener.onTelemetryReceived(json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onConnectionStateChanged(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onConnectionStateChanged(false)
            }
        })
    }

    fun sendUtterance(utterance: String) {
        val payload = JSONObject().apply {
            put("utterance", utterance)
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed")
        webSocket = null
    }
}
