package com.cherryblossomdev.greywire.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Chat endpoints (Breakroom/backend/routes/chat.js). With X-Site: greywire,
// /rooms lists the Greywire community's rooms. MariaDB booleans arrive as 1/0,
// so flags the app doesn't use are left out.

/** A group room (from /rooms) or a direct message (from /dms). */
data class ChatRoom(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    /** "room" or "dm"; /rooms doesn't send it. */
    val type: String? = null,
    @SerializedName("owner_handle") val ownerHandle: String? = null,
    // Direct messages only
    @SerializedName("partner_id") val partnerId: Int? = null,
    @SerializedName("partner_handle") val partnerHandle: String? = null,
    @SerializedName("unread_count") val unreadCount: Int? = null,
    @SerializedName("last_message") val lastMessage: String? = null,
    @SerializedName("last_message_at") val lastMessageAt: String? = null
) {
    val isDirectMessage: Boolean get() = type == "dm" || partnerHandle != null

    val displayName: String
        get() = if (isDirectMessage) partnerHandle ?: "Unknown" else name ?: "Chat"
}

data class ChatMessage(
    val id: Int,
    val message: String? = null,
    @SerializedName("image_path") val imagePath: String? = null,
    @SerializedName("video_path") val videoPath: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("user_id") val userId: Int? = null,
    val handle: String? = null
) {
    val hasMedia: Boolean get() = imagePath != null || videoPath != null
}

data class ChatRoomsResponse(val rooms: List<ChatRoom> = emptyList())

data class ChatDmsResponse(val dms: List<ChatRoom> = emptyList())

/** Oldest first. [hasMore]: there are older messages before the first one. */
data class ChatMessagesResponse(
    val messages: List<ChatMessage> = emptyList(),
    val hasMore: Boolean = false
)

data class SendMessageRequest(val message: String)

/** POST /rooms/:id/messages answers `{ message: <the new message> }`. */
data class SendMessageResponse(val message: ChatMessage? = null)

interface ChatApiService {

    @GET("api/chat/rooms")
    suspend fun getRooms(@Header("Authorization") token: String): Response<ChatRoomsResponse>

    @GET("api/chat/dms")
    suspend fun getDms(@Header("Authorization") token: String): Response<ChatDmsResponse>

    /** The newest [limit] messages, or those before [before] (a message's created_at). */
    @GET("api/chat/rooms/{roomId}/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: Int,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<ChatMessagesResponse>

    @POST("api/chat/rooms/{roomId}/messages")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: Int,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    @POST("api/chat/rooms/{roomId}/mark-read")
    suspend fun markRead(@Header("Authorization") token: String, @Path("roomId") roomId: Int): Response<MessageResponse>
}
