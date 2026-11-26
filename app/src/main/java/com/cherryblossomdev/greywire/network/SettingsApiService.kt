package com.cherryblossomdev.greywire.network

import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

// Account settings (the web's Profile > Settings page, ProfileSettings.vue):
// notification preferences (Breakroom/backend/routes/user.js).

/**
 * Notification preferences (`user_settings`). With no row yet, the backend
 * answers with everything on. `PUT` replaces all four.
 */
data class NotificationSettings(
    @SerializedName("notifications_enabled") @JsonAdapter(LenientBooleanAdapter::class)
    val notificationsEnabled: Boolean = true,
    @SerializedName("notify_chat_messages") @JsonAdapter(LenientBooleanAdapter::class)
    val notifyChatMessages: Boolean = true,
    @SerializedName("notify_friend_requests") @JsonAdapter(LenientBooleanAdapter::class)
    val notifyFriendRequests: Boolean = true,
    /** "Comments on your content" on the web. */
    @SerializedName("notify_blog_comments") @JsonAdapter(LenientBooleanAdapter::class)
    val notifyComments: Boolean = true
)

/**
 * Reads a boolean that MariaDB may send as a number (TINYINT 0/1) as well as
 * true/false; null reads as false.
 */
class LenientBooleanAdapter : TypeAdapter<Boolean>() {
    override fun write(out: JsonWriter, value: Boolean?) {
        out.value(value ?: false)
    }

    override fun read(reader: JsonReader): Boolean = when (reader.peek()) {
        JsonToken.BOOLEAN -> reader.nextBoolean()
        JsonToken.NUMBER -> reader.nextInt() != 0
        JsonToken.STRING -> reader.nextString().let { it == "1" || it.equals("true", ignoreCase = true) }
        JsonToken.NULL -> {
            reader.nextNull()
            false
        }
        else -> {
            reader.skipValue()
            false
        }
    }
}

interface SettingsApiService {

    @GET("api/user/notification-settings")
    suspend fun getNotificationSettings(@Header("Authorization") token: String): Response<NotificationSettings>

    @PUT("api/user/notification-settings")
    suspend fun updateNotificationSettings(
        @Header("Authorization") token: String,
        @Body body: NotificationSettings
    ): Response<MessageOnlyResponse>
}
