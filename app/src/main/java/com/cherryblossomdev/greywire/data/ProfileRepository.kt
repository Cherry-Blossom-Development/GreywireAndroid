package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.MessageOnlyResponse
import com.cherryblossomdev.greywire.network.ProfileApiService
import com.cherryblossomdev.greywire.network.ProfileResponse
import com.cherryblossomdev.greywire.network.ProfileUpdate

class ProfileRepository(
    private val api: ProfileApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getProfile(): AuthResult<ProfileResponse> =
        authedCall(tokenManager, "Couldn't load your profile") { api.getProfile(it) }

    suspend fun updateProfile(update: ProfileUpdate): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, "Couldn't save your profile") { api.updateProfile(it, update) }
}
