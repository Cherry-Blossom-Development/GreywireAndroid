package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.GreywireApiService
import com.cherryblossomdev.greywire.network.MembershipResponse

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
}
