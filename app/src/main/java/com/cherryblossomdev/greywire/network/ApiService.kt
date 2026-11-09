package com.cherryblossomdev.greywire.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// Auth endpoints -- the same Breakroom backend routes the Prosaurus app uses
// (Breakroom/backend/routes/authentication.js).

data class LoginRequest(
    // The backend matches this against handle, email or alternate email
    val handle: String,
    val password: String
)

data class AuthResponse(
    val message: String,
    val token: String? = null
)

data class MeResponse(
    val username: String,
    val userId: Int,
    val isGuest: Boolean = false,
    val greywireMember: Boolean = false
)

data class ErrorResponse(
    val message: String
)

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<MeResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<AuthResponse>
}
