package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.ProfileApiService
import com.cherryblossomdev.greywire.network.ProfileResponse

class ProfileRepository(
    private val api: ProfileApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getProfile(): AuthResult<ProfileResponse> =
        authedCall(tokenManager, "Couldn't load your profile") { api.getProfile(it) }
}
