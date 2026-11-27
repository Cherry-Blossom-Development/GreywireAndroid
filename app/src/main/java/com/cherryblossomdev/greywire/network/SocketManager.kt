package com.cherryblossomdev.greywire.network

import android.util.Log
import com.cherryblossomdev.greywire.data.TokenManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Live chat events from the backend's Socket.IO server (Breakroom/backend/utilities/socket.js). */
sealed interface ChatSocketEvent {
    val roomId: Int

    data class NewMessage(override val roomId: Int, val message: ChatMessage) : ChatSocketEvent
    data class MessageEdited(override val roomId: Int, val message: ChatMessage) : ChatSocketEvent
    data class MessageDeleted(override val roomId: Int, val messageId: Int) : ChatSocketEvent
}

enum class SocketState { Disconnected, Connecting, Connected, Reconnecting }

/**
 * One Socket.IO connection for the signed-in user, as the Prosaurus app has.
 * Rooms joined with [joinRoom] are joined again after a reconnect. The
 * handshake sends the token and `X-Site: greywire`, so the backend treats
 * the connection as Greywire. Sending still goes over REST (ChatRepository),
 * which reports errors; the backend broadcasts those messages here too.
 */
class SocketManager(private val tokenManager: TokenManager) {

    private val gson = Gson()
    private var socket: Socket? = null
    private val joinedRooms = mutableSetOf<Int>()

    private val _state = MutableStateFlow(SocketState.Disconnected)
    val state: StateFlow<SocketState> = _state

    private val _events = MutableSharedFlow<ChatSocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ChatSocketEvent> = _events

    /** Connects if not already connected (or connecting). */
    @Synchronized
    fun connect() {
        if (socket != null) return
        val token = tokenManager.getToken() ?: return

        val options = IO.Options().apply {
            auth = mapOf("token" to token)
            extraHeaders = mapOf(
                "X-Site" to listOf("greywire"),
                "X-Client-Platform" to listOf("android")
            )
            reconnection = true
            reconnectionDelay = 1000
            reconnectionDelayMax = 10_000
            callFactory = RetrofitClient.socketHttpClient
            webSocketFactory = RetrofitClient.socketHttpClient
        }
        _state.value = SocketState.Connecting
        socket = IO.socket(RetrofitClient.BASE_URL, options).apply {
            on(Socket.EVENT_CONNECT, onConnect)
            on(Socket.EVENT_DISCONNECT) { _state.value = SocketState.Reconnecting }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.w(TAG, "Connect error: ${args.firstOrNull()}")
                _state.value = SocketState.Reconnecting
            }
            on("new_message") { args -> parseMessage(args)?.let { (room, msg) -> _events.tryEmit(ChatSocketEvent.NewMessage(room, msg)) } }
            on("message_edited") { args -> parseMessage(args)?.let { (room, msg) -> _events.tryEmit(ChatSocketEvent.MessageEdited(room, msg)) } }
            on("message_deleted", onMessageDeleted)
            connect()
        }
    }

    /** Disconnects and forgets the joined rooms (on sign out). */
    @Synchronized
    fun disconnect() {
        joinedRooms.clear()
        socket?.off()
        socket?.disconnect()
        socket = null
        _state.value = SocketState.Disconnected
    }

    /** Joins a chat room's broadcasts, connecting first if needed. */
    @Synchronized
    fun joinRoom(roomId: Int) {
        joinedRooms.add(roomId)
        if (socket == null) connect() else if (socket?.connected() == true) socket?.emit("join_room", roomId)
        // When not connected yet, onConnect joins it
    }

    @Synchronized
    fun leaveRoom(roomId: Int) {
        joinedRooms.remove(roomId)
        if (socket?.connected() == true) socket?.emit("leave_room", roomId)
    }

    private val onConnect = Emitter.Listener {
        _state.value = SocketState.Connected
        synchronized(this) { joinedRooms.toList() }.forEach { socket?.emit("join_room", it) }
    }

    private val onMessageDeleted = Emitter.Listener { args ->
        val data = payload(args) ?: return@Listener
        val roomId = data.intOrNull("roomId") ?: return@Listener
        val messageId = data.intOrNull("messageId") ?: return@Listener
        _events.tryEmit(ChatSocketEvent.MessageDeleted(roomId, messageId))
    }

    /** `{ roomId, message }`, as `new_message` and `message_edited` send it. */
    private fun parseMessage(args: Array<Any?>): Pair<Int, ChatMessage>? {
        val data = payload(args) ?: return null
        val roomId = data.intOrNull("roomId") ?: return null
        val message = data.getAsJsonObject("message") ?: return null
        return try {
            roomId to gson.fromJson(message, ChatMessage::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read a chat message", e)
            null
        }
    }

    private fun payload(args: Array<Any?>): JsonObject? = try {
        args.firstOrNull()?.let { JsonParser.parseString(it.toString()).asJsonObject }
    } catch (e: Exception) {
        Log.w(TAG, "Couldn't read a socket event", e)
        null
    }

    /** The backend sends room ids as numbers or strings, depending on the sender. */
    private fun JsonObject.intOrNull(key: String): Int? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull()

    private companion object {
        const val TAG = "SocketManager"
    }
}
