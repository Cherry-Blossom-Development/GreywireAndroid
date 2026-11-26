package com.cherryblossomdev.greywire

import android.content.Context
import com.cherryblossomdev.greywire.data.AuthRepository
import com.cherryblossomdev.greywire.data.BillingRepository
import com.cherryblossomdev.greywire.data.ChatRepository
import com.cherryblossomdev.greywire.data.FriendsRepository
import com.cherryblossomdev.greywire.data.GreywireRepository
import com.cherryblossomdev.greywire.data.ProfileRepository
import com.cherryblossomdev.greywire.data.SessionsRepository
import com.cherryblossomdev.greywire.data.SettingsRepository
import com.cherryblossomdev.greywire.data.TokenManager
import com.cherryblossomdev.greywire.network.RetrofitClient

// Simple manual dependency container, as in the Prosaurus app.
class AppContainer(context: Context) {
    val tokenManager by lazy { TokenManager(context) }

    init {
        // Whenever the backend sends a refreshed token, persist it
        RetrofitClient.tokenUpdateCallback = { token -> tokenManager.saveToken(token) }
    }

    val authRepository by lazy { AuthRepository(RetrofitClient.apiService, tokenManager) }
    val greywireRepository by lazy { GreywireRepository(RetrofitClient.greywireApiService, tokenManager) }
    val billingRepository by lazy { BillingRepository(RetrofitClient.billingApiService, tokenManager) }
    val chatRepository by lazy { ChatRepository(RetrofitClient.chatApiService, tokenManager) }
    val sessionsRepository by lazy { SessionsRepository(RetrofitClient.sessionsApiService, RetrofitClient.sessionsUploadApiService, tokenManager) }
    val friendsRepository by lazy { FriendsRepository(RetrofitClient.friendsApiService, tokenManager) }
    val settingsRepository by lazy { SettingsRepository(RetrofitClient.settingsApiService, tokenManager) }
    val profileRepository by lazy { ProfileRepository(RetrofitClient.profileApiService, RetrofitClient.profileUploadApiService, tokenManager) }
}
