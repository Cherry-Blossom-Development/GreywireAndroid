package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.AlternateEmail
import com.cherryblossomdev.greywire.network.AlternateEmailNotifyUpdate
import com.cherryblossomdev.greywire.network.AlternateEmailUpdate
import com.cherryblossomdev.greywire.network.MessageOnlyResponse
import com.cherryblossomdev.greywire.network.NotificationSettings
import com.cherryblossomdev.greywire.network.SettingsApiService

/** Account settings: notification preferences and the alternate email. */
class SettingsRepository(
    private val api: SettingsApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getNotificationSettings(): AuthResult<NotificationSettings> =
        authedCall(tokenManager, "Couldn't load your notification settings") { api.getNotificationSettings(it) }

    suspend fun updateNotificationSettings(settings: NotificationSettings): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, "Couldn't save your notification settings") { api.updateNotificationSettings(it, settings) }

    suspend fun getAlternateEmail(): AuthResult<AlternateEmail> =
        authedCall(tokenManager, "Couldn't load your alternate email") { api.getAlternateEmail(it) }

    /** Sets the address (and sends its confirmation email); an empty one removes it. */
    suspend fun setAlternateEmail(address: String): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, if (address.isEmpty()) "Couldn't remove the alternate email" else "Couldn't set the alternate email") {
            api.setAlternateEmail(it, AlternateEmailUpdate(address))
        }

    suspend fun resendAlternateEmail(): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, "Couldn't resend the confirmation email") { api.resendAlternateEmail(it) }

    suspend fun requestAccountDeletion(): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, "Couldn't send the deletion request. Please try again.") { api.requestAccountDeletion(it) }

    suspend fun setAlternateEmailNotify(enabled: Boolean): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, "Couldn't save the setting") { api.setAlternateEmailNotify(it, AlternateEmailNotifyUpdate(enabled)) }
}
