package com.cherryblossomdev.greywire

import android.content.Context
import com.cherryblossomdev.greywire.data.AuthRepository
import com.cherryblossomdev.greywire.data.GreywireRepository
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
}
