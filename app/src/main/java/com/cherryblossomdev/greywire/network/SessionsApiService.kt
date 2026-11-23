package com.cherryblossomdev.greywire.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// Sessions endpoints (Breakroom/backend/routes/sessions.js). With X-Site:
// greywire the lists hold Greywire's sessions only (sessions.site).

/** A recording. Ratings are 1 to 10; [avgRating] is null until someone rates it. */
data class Session(
    val id: Int,
    val name: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("uploaded_at") val uploadedAt: String? = null,
    @SerializedName("recorded_at") val recordedAt: String? = null,
    /** "band", "individual" or "mashup". */
    @SerializedName("session_type") val sessionType: String? = null,
    @SerializedName("band_id") val bandId: Int? = null,
    @SerializedName("band_name") val bandName: String? = null,
    @SerializedName("instrument_id") val instrumentId: Int? = null,
    @SerializedName("instrument_name") val instrumentName: String? = null,
    @SerializedName("duration_ms") val durationMs: Long? = null,
    @SerializedName("avg_rating") val avgRating: Double? = null,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("my_rating") val myRating: Int? = null,
    @SerializedName("shortlist_count") val shortlistCount: Int = 0,
    /** Band members' sessions only. */
    @SerializedName("uploader_handle") val uploaderHandle: String? = null
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: "Untitled Session"

    val typeLabel: String
        get() = when (sessionType) {
            "band" -> "Band"
            "individual" -> "Solo"
            "mashup" -> "Mashup"
            else -> "Recording"
        }

    /** "3:07", or "--:--" when the length isn't known (older uploads). */
    val formattedDuration: String
        get() = durationMs?.takeIf { it > 0 }?.let { formatDuration(it) } ?: "--:--"
}

/** "m:ss" for a length in milliseconds. */
fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

data class SessionsResponse(val sessions: List<Session> = emptyList())

/** null clears the user's rating. */
data class RateSessionRequest(val rating: Int?)

data class SessionRatingResponse(
    @SerializedName("avg_rating") val avgRating: Double? = null,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("my_rating") val myRating: Int? = null
)

interface SessionsApiService {

    /** The user's own sessions. */
    @GET("api/sessions")
    suspend fun getSessions(@Header("Authorization") token: String): Response<SessionsResponse>

    /** Sessions uploaded by the user's bandmates in shared bands. */
    @GET("api/sessions/band-members")
    suspend fun getBandMemberSessions(@Header("Authorization") token: String): Response<SessionsResponse>

    @POST("api/sessions/{id}/rate")
    suspend fun rate(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: RateSessionRequest
    ): Response<SessionRatingResponse>
}
