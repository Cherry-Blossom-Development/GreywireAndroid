package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.ChatApiService
import com.cherryblossomdev.greywire.network.ChatDmsResponse
import com.cherryblossomdev.greywire.network.ChatMessage
import com.cherryblossomdev.greywire.network.ChatMessagesResponse
import com.cherryblossomdev.greywire.network.ChatRoomsResponse
import com.cherryblossomdev.greywire.network.MessageResponse
import com.cherryblossomdev.greywire.network.SendMessageRequest
import com.cherryblossomdev.greywire.network.SocketManager

// Chat: messages load over REST when a room opens and on "Load earlier
// messages", and sending is REST too. While a room is open, new, edited and
// deleted messages arrive live over Socket.IO ([socket]).
class ChatRepository(
    private val api: ChatApiService,
    private val tokenManager: TokenManager,
    val socket: SocketManager
) {
    suspend fun getRooms(): AuthResult<ChatRoomsResponse> =
        authedCall(tokenManager, "Couldn't load chat rooms") { api.getRooms(it) }

    suspend fun getDms(): AuthResult<ChatDmsResponse> =
        authedCall(tokenManager, "Couldn't load your messages") { api.getDms(it) }

    suspend fun getMessages(roomId: Int, before: String? = null): AuthResult<ChatMessagesResponse> =
        authedCall(tokenManager, "Couldn't load messages") { api.getMessages(it, roomId, before = before) }

    suspend fun sendMessage(roomId: Int, text: String): AuthResult<ChatMessage> =
        when (val result = authedCall(tokenManager, "Couldn't send the message") {
            api.sendMessage(it, roomId, SendMessageRequest(text))
        }) {
            is AuthResult.Success -> result.data.message?.let { AuthResult.Success(it) }
                ?: AuthResult.Error("Couldn't send the message")
            is AuthResult.Error -> result
        }

    suspend fun markRead(roomId: Int): AuthResult<MessageResponse> =
        authedCall(tokenManager, "Couldn't mark the room read") { api.markRead(it, roomId) }
}
