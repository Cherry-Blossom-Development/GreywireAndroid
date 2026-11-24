package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.BandsResponse
import com.cherryblossomdev.greywire.network.InstrumentsResponse
import com.cherryblossomdev.greywire.network.MessageResponse
import com.cherryblossomdev.greywire.network.RateSessionRequest
import com.cherryblossomdev.greywire.network.RetrofitClient
import com.cherryblossomdev.greywire.network.SessionRatingResponse
import com.cherryblossomdev.greywire.network.SessionResponse
import com.cherryblossomdev.greywire.network.SessionsApiService
import com.cherryblossomdev.greywire.network.SessionsResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/** The largest recording the API takes: nginx's client_max_body_size (100 MB). */
const val MAX_UPLOAD_MB = 100

/** A little under the limit, leaving room for the form's other fields. */
const val MAX_UPLOAD_BYTES = (MAX_UPLOAD_MB - 1) * 1024L * 1024L

/** The message for a file over [MAX_UPLOAD_MB], the same as the Prosaurus app's. */
fun tooLargeMessage(size: Long): String {
    val mb = Math.round(size / (1024.0 * 1024.0))
    return "This file is $mb MB, which is over the $MAX_UPLOAD_MB MB upload limit. " +
        "Try exporting it as an MP3 or another compressed format, or split it into shorter recordings."
}

class SessionsRepository(
    private val api: SessionsApiService,
    private val uploadApi: SessionsApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getSessions(): AuthResult<SessionsResponse> =
        authedCall(tokenManager, "Couldn't load your sessions") { api.getSessions(it) }

    suspend fun getBandMemberSessions(): AuthResult<SessionsResponse> =
        authedCall(tokenManager, "Couldn't load band sessions") { api.getBandMemberSessions(it) }

    /** Rates a session 1 to 10, or clears the user's rating with null. */
    suspend fun rate(sessionId: Int, rating: Int?): AuthResult<SessionRatingResponse> =
        authedCall(tokenManager, "Couldn't save your rating") { api.rate(it, sessionId, RateSessionRequest(rating)) }

    suspend fun getBands(): AuthResult<BandsResponse> =
        authedCall(tokenManager, "Couldn't load your bands") { api.getBands(it) }

    suspend fun getInstruments(): AuthResult<InstrumentsResponse> =
        authedCall(tokenManager, "Couldn't load instruments") { api.getInstruments(it) }

    /**
     * Uploads [file] as a new session. [sessionType] is "band" or "individual";
     * [recordedAt] is "YYYY-MM-DD" or null. A 402 (free-tier limit) comes back
     * as an error with requiresSubscription.
     */
    suspend fun upload(
        file: File,
        fileName: String,
        mimeType: String,
        name: String,
        sessionType: String,
        recordedAt: String?,
        bandId: Int?,
        instrumentId: Int?
    ): AuthResult<SessionResponse> {
        if (file.length() > MAX_UPLOAD_BYTES) return AuthResult.Error(tooLargeMessage(file.length()))
        val text = "text/plain".toMediaTypeOrNull()
        val audio = MultipartBody.Part.createFormData("audio", fileName, file.asRequestBody(mimeType.toMediaTypeOrNull()))
        return authedCall(tokenManager, "Couldn't upload the recording") {
            uploadApi.upload(
                it,
                audio,
                name.toRequestBody(text),
                sessionType.toRequestBody(text),
                recordedAt?.toRequestBody(text),
                bandId?.toString()?.toRequestBody(text),
                instrumentId?.toString()?.toRequestBody(text)
            )
        }
    }

    /** Deletes one of the user's own sessions. */
    suspend fun delete(sessionId: Int): AuthResult<MessageResponse> =
        authedCall(tokenManager, "Couldn't delete the session") { api.delete(it, sessionId) }

    /** The playback stream (the compressed copy; supports range requests for seeking). */
    fun streamUrl(sessionId: Int): String = "${RetrofitClient.BASE_URL}api/sessions/$sessionId/stream"

    /** An HTTP client for the player that sends the user's token, like the API calls. */
    fun mediaHttpClient(): OkHttpClient = RetrofitClient.mediaHttpClient(tokenManager.getBearerToken())
}
