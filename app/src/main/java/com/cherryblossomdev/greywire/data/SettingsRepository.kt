package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.MessageOnlyResponse
import com.cherryblossomdev.greywire.network.NotificationSettings
import com.cherryblossomdev.greywire.network.SettingsApiService

/** Account settings: notification preferences. */
class SettingsRepository(
    private val api: SettingsApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getNotificationSettings(): AuthResult<NotificationSettings> =
        authedCall(tokenManager, "Couldn't load your notification settings") { api.getNotificationSettings(it) }

    suspend fun updateNotificationSettings(settings: NotificationSettings): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, "Couldn't save your notification settings") { api.updateNotificationSettings(it, settings) }
}
