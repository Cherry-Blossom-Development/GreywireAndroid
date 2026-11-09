package com.cherryblossomdev.greywire.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Stores the login JWT in encrypted prefs, as the Prosaurus app does.
// The prefs file name is excluded from backups (res/xml/backup_rules.xml).
class TokenManager(private val context: Context) {

    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        const val PREFS_NAME = "greywire_secure_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "username"
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun getToken(): String? = sharedPreferences.getString(KEY_JWT_TOKEN, null)

    fun getBearerToken(): String? = getToken()?.let { "Bearer $it" }

    fun saveUsername(username: String) {
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? = sharedPreferences.getString(KEY_USERNAME, null)

    fun clearAll() {
        sharedPreferences.edit()
            .remove(KEY_JWT_TOKEN)
            .remove(KEY_USERNAME)
            .apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null
}
