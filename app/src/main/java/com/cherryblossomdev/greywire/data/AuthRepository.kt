package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.ApiService
import com.cherryblossomdev.greywire.network.AuthResponse
import com.cherryblossomdev.greywire.network.ErrorResponse
import com.cherryblossomdev.greywire.network.LoginRequest
import com.cherryblossomdev.greywire.network.MeResponse
import com.google.gson.Gson

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val unauthorized: Boolean = false) : AuthResult<Nothing>()
}

// Same login mechanism as the Prosaurus app: POST /api/auth/login with the
// plain password over HTTPS, keep the returned JWT, and send it as a Bearer
// header (plus the jwtToken cookie, via RetrofitClient) on later calls.
class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    suspend fun login(handle: String, password: String): AuthResult<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(handle.trim(), password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.token != null) {
                    tokenManager.saveToken(body.token)
                    // Provisional: what the user typed may be an email. getMe()
                    // replaces it with the real handle.
                    tokenManager.saveUsername(handle.trim())
                    AuthResult.Success(body)
                } else {
                    AuthResult.Error("No token received")
                }
            } else {
                AuthResult.Error(parseError(response.errorBody()?.string(), "Login failed"))
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMe(): AuthResult<MeResponse> {
        val bearerToken = tokenManager.getBearerToken()
            ?: return AuthResult.Error("Not logged in", unauthorized = true)

        return try {
            val response = apiService.getMe(bearerToken)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                tokenManager.saveUsername(body.username)
                AuthResult.Success(body)
            } else {
                AuthResult.Error("Failed to get user info", unauthorized = response.code() == 401)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout() {
        val bearerToken = tokenManager.getBearerToken()
        // Clear locally first so the user is signed out even if the call fails
        tokenManager.clearAll()
        try {
            if (bearerToken != null) apiService.logout(bearerToken)
        } catch (e: Exception) { /* signed out locally; nothing else to do */ }
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun getStoredUsername(): String? = tokenManager.getUsername()

    private fun parseError(errorBody: String?, fallback: String): String = try {
        Gson().fromJson(errorBody, ErrorResponse::class.java)?.message ?: fallback
    } catch (e: Exception) {
        fallback
    }
}
