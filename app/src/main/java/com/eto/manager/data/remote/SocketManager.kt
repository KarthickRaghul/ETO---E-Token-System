package com.eto.manager.data.remote

import io.socket.client.IO
import io.socket.client.Socket
import android.util.Log

object SocketManager {
    private const val SOCKET_URL = "http://10.0.2.2:3000"
    private var socket: Socket? = null

    fun connect(onQueueUpdate: () -> Unit) {
        try {
            if (socket == null) {
                socket = IO.socket(SOCKET_URL)
            }
            socket?.on("queue_update") {
                Log.d("SocketManager", "Received queue_update event")
                onQueueUpdate()
            }
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketManager", "Socket connected successfully")
            }
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketManager", "Socket disconnected")
            }
            socket?.connect()
        } catch (e: Exception) {
            Log.e("SocketManager", "Error connecting socket", e)
        }
    }

    fun disconnect() {
        socket?.disconnect()
    }
}
