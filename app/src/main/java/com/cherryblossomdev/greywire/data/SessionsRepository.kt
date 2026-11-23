package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.RateSessionRequest
import com.cherryblossomdev.greywire.network.RetrofitClient
import com.cherryblossomdev.greywire.network.SessionRatingResponse
import com.cherryblossomdev.greywire.network.SessionsApiService
import com.cherryblossomdev.greywire.network.SessionsResponse
import okhttp3.OkHttpClient

class SessionsRepository(
    private val api: SessionsApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getSessions(): AuthResult<SessionsResponse> =
        authedCall(tokenManager, "Couldn't load your sessions") { api.getSessions(it) }

    suspend fun getBandMemberSessions(): AuthResult<SessionsResponse> =
        authedCall(tokenManager, "Couldn't load band sessions") { api.getBandMemberSessions(it) }

    /** Rates a session 1 to 10, or clears the user's rating with null. */
    suspend fun rate(sessionId: Int, rating: Int?): AuthResult<SessionRatingResponse> =
        authedCall(tokenManager, "Couldn't save your rating") { api.rate(it, sessionId, RateSessionRequest(rating)) }

    /** The playback stream (the compressed copy; supports range requests for seeking). */
    fun streamUrl(sessionId: Int): String = "${RetrofitClient.BASE_URL}api/sessions/$sessionId/stream"

    /** An HTTP client for the player that sends the user's token, like the API calls. */
    fun mediaHttpClient(): OkHttpClient = RetrofitClient.mediaHttpClient(tokenManager.getBearerToken())
}
