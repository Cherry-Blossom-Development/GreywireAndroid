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
import retrofit2.http.POST
import retrofit2.http.PUT

// Account settings (the web's Profile > Settings page, ProfileSettings.vue):
// notification preferences and the alternate email (Breakroom/backend/routes/user.js).

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
 * A second address that account notices can also go to. A new or changed
 * address starts unconfirmed and gets a confirmation email (the link opens
 * the website); notices go there only once it's confirmed and switched on.
 */
data class AlternateEmail(
    @SerializedName("alternate_email") val address: String? = null,
    @SerializedName("alternate_email_verified") @JsonAdapter(LenientBooleanAdapter::class)
    val verified: Boolean = false,
    @SerializedName("send_notices_to_alternate_email") @JsonAdapter(LenientBooleanAdapter::class)
    val sendNotices: Boolean = false
)

/** Sets the alternate email; an empty address removes it. */
data class AlternateEmailUpdate(@SerializedName("alternate_email") val address: String)

data class AlternateEmailNotifyUpdate(val enabled: Boolean)

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

    @GET("api/user/alternate-email")
    suspend fun getAlternateEmail(@Header("Authorization") token: String): Response<AlternateEmail>

    /** Sends the confirmation email (400 for an invalid address, 409 if another account uses it). */
    @PUT("api/user/alternate-email")
    suspend fun setAlternateEmail(
        @Header("Authorization") token: String,
        @Body body: AlternateEmailUpdate
    ): Response<MessageOnlyResponse>

    @POST("api/user/alternate-email/resend")
    suspend fun resendAlternateEmail(@Header("Authorization") token: String): Response<MessageOnlyResponse>

    /**
     * Asks an administrator to delete the account and all its data
     * (409 if a request is already pending). Nothing is deleted straight away.
     */
    @POST("api/profile/deletion-request")
    suspend fun requestAccountDeletion(@Header("Authorization") token: String): Response<MessageOnlyResponse>

    /** Turning it on needs a confirmed address (400 otherwise). */
    @PUT("api/user/alternate-email/notify")
    suspend fun setAlternateEmailNotify(
        @Header("Authorization") token: String,
        @Body body: AlternateEmailNotifyUpdate
    ): Response<MessageOnlyResponse>
}
