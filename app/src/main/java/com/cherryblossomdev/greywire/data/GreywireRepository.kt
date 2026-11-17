package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.ClaimPedalRequest
import com.cherryblossomdev.greywire.network.ClaimPedalResponse
import com.cherryblossomdev.greywire.network.ErrorResponse
import com.cherryblossomdev.greywire.network.GreywireApiService
import com.cherryblossomdev.greywire.network.MembershipResponse
import com.google.gson.Gson

class GreywireRepository(
    private val api: GreywireApiService,
    private val tokenManager: TokenManager
) {

    suspend fun getMembership(): AuthResult<MembershipResponse> {
        val token = tokenManager.getBearerToken()
            ?: return AuthResult.Error("Not logged in", unauthorized = true)
        return try {
            val response = api.getMembership(token)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                AuthResult.Success(body)
            } else {
                AuthResult.Error("Couldn't load your membership", unauthorized = response.code() == 401)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * Claims a pedal by serial, which makes the user a member. The backend
     * normalizes the serial (spaces, dashes, case) and its error messages are
     * written for users, so they're passed through: bad format (400), unknown
     * serial (404), already claimed by someone else (409), guest account (403),
     * too many failed attempts (429).
     */
    suspend fun claimPedal(serial: String): AuthResult<ClaimPedalResponse> {
        val token = tokenManager.getBearerToken()
            ?: return AuthResult.Error("Not logged in", unauthorized = true)
        return try {
            val response = api.claimPedal(token, ClaimPedalRequest(serial))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                AuthResult.Success(body)
            } else {
                AuthResult.Error(
                    parseError(response.errorBody()?.string(), "Couldn't claim the pedal"),
                    unauthorized = response.code() == 401
                )
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    private fun parseError(errorBody: String?, fallback: String): String = try {
        Gson().fromJson(errorBody, ErrorResponse::class.java)?.message ?: fallback
    } catch (e: Exception) {
        fallback
    }
}
