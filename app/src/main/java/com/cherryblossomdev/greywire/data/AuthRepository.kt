package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.ApiService
import com.cherryblossomdev.greywire.network.AuthResponse
import com.cherryblossomdev.greywire.network.EulaAcceptResponse
import com.cherryblossomdev.greywire.network.EulaStatusResponse
import com.cherryblossomdev.greywire.network.ErrorResponse
import com.cherryblossomdev.greywire.network.ForgotPasswordRequest
import com.cherryblossomdev.greywire.network.LoginRequest
import com.cherryblossomdev.greywire.network.MeResponse
import com.cherryblossomdev.greywire.network.SignupRequest
import com.google.gson.Gson
import java.security.MessageDigest
import java.security.SecureRandom

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    /**
     * [unauthorized]: the token was rejected (401), so the user must sign in again.
     * [requiresSubscription]: a free-tier limit was reached (402); show the paywall.
     */
    data class Error(
        val message: String,
        val unauthorized: Boolean = false,
        val requiresSubscription: Boolean = false
    ) : AuthResult<Nothing>()
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

    /**
     * Creates an account and signs it in. With X-Site: greywire the backend
     * records Greywire as the signup site and sends Greywire's verification email.
     */
    suspend fun signup(
        handle: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AuthResult<AuthResponse> {
        return try {
            val salt = generateSalt()
            val response = apiService.signup(
                SignupRequest(
                    handle = handle.trim(),
                    first_name = firstName.trim(),
                    last_name = lastName.trim(),
                    email = email.trim(),
                    hash = hashPassword(password, salt),
                    salt = salt
                )
            )
            val body = response.body()
            if (response.isSuccessful && body?.token != null) {
                tokenManager.saveToken(body.token)
                tokenManager.saveUsername(handle.trim())
                AuthResult.Success(body)
            } else if (response.isSuccessful) {
                AuthResult.Error("Account created, but no token was received. Try signing in.")
            } else {
                AuthResult.Error(parseError(response.errorBody()?.string(), "Couldn't create the account"))
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * Asks the backend to email a password reset link. With X-Site: greywire it
     * sends Greywire's template, whose link opens the Greywire website.
     */
    suspend fun forgotPassword(email: String): AuthResult<AuthResponse> {
        return try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email.trim()))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                AuthResult.Success(body)
            } else {
                AuthResult.Error(parseError(response.errorBody()?.string(), "Couldn't send the reset link"))
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

    suspend fun getEulaStatus(): AuthResult<EulaStatusResponse> {
        val bearerToken = tokenManager.getBearerToken()
            ?: return AuthResult.Error("Not logged in", unauthorized = true)
        return try {
            val response = apiService.getEulaStatus(bearerToken)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                AuthResult.Success(body)
            } else {
                AuthResult.Error("Couldn't load EULA status", unauthorized = response.code() == 401)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun acceptEula(): AuthResult<EulaAcceptResponse> {
        val bearerToken = tokenManager.getBearerToken()
            ?: return AuthResult.Error("Not logged in", unauthorized = true)
        return try {
            val response = apiService.acceptEula(bearerToken)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                AuthResult.Success(body)
            } else {
                AuthResult.Error(parseError(response.errorBody()?.string(), "Couldn't accept the EULA"),
                    unauthorized = response.code() == 401)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun getStoredUsername(): String? = tokenManager.getUsername()

    // Password hashing, matching the Prosaurus app and the website (SignupForm.vue)
    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((password + salt).toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun parseError(errorBody: String?, fallback: String): String = try {
        Gson().fromJson(errorBody, ErrorResponse::class.java)?.message ?: fallback
    } catch (e: Exception) {
        fallback
    }
}
