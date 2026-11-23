package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.ErrorResponse
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response

/**
 * Runs a signed-in API call and turns the response into an [AuthResult]:
 * no saved token or a 401 is `unauthorized` (sign the user out), a 402 is
 * `requiresSubscription` (show the paywall), and the backend's own error
 * message is passed through when it sends one.
 */
internal suspend fun <T> authedCall(
    tokenManager: TokenManager,
    fallbackError: String,
    call: suspend (bearerToken: String) -> Response<T>
): AuthResult<T> {
    val token = tokenManager.getBearerToken()
        ?: return AuthResult.Error("Not logged in", unauthorized = true)
    return try {
        val response = call(token)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            AuthResult.Success(body)
        } else {
            AuthResult.Error(
                parseErrorMessage(response.errorBody()?.string(), fallbackError),
                unauthorized = response.code() == 401,
                requiresSubscription = response.code() == 402
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Network error")
    }
}

/** The backend's `{ message }` from an error body, or [fallback]. */
internal fun parseErrorMessage(errorBody: String?, fallback: String): String = try {
    Gson().fromJson(errorBody, ErrorResponse::class.java)?.message ?: fallback
} catch (e: Exception) {
    fallback
}
