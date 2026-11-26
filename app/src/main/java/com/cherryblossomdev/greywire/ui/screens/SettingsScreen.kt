package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.SettingsRepository
import com.cherryblossomdev.greywire.network.NotificationSettings
import com.cherryblossomdev.greywire.ui.components.ErrorText
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireSubScreen
import com.cherryblossomdev.greywire.ui.components.LoadingFill
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Settings (roadmap 2.9), opened from the Profile tab, matching the web's
// Profile > Settings page: notification preferences.

data class SettingsUiState(
    val notifications: NotificationSettings? = null,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val notificationsError: String? = null,
    val sessionExpired: Boolean = false
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    // Saves go one at a time, so quick taps reach the server in order
    private val saveLock = Mutex()

    init {
        viewModelScope.launch {
            _uiState.value = when (val result = repository.getNotificationSettings()) {
                is AuthResult.Success -> _uiState.value.copy(notifications = result.data, isLoading = false)
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    sessionExpired = result.unauthorized,
                    loadError = result.message
                )
            }
        }
    }

    /** Shows the change at once and saves it, as the web does; puts it back if saving fails. */
    fun updateNotifications(change: (NotificationSettings) -> NotificationSettings) {
        val before = _uiState.value.notifications ?: return
        val after = change(before)
        _uiState.value = _uiState.value.copy(notifications = after, notificationsError = null)
        viewModelScope.launch {
            val result = saveLock.withLock { repository.updateNotificationSettings(after) }
            if (result is AuthResult.Error) {
                // Only roll back if nothing newer has been shown since
                val current = _uiState.value.notifications
                _uiState.value = _uiState.value.copy(
                    notifications = if (current == after) before else current,
                    sessionExpired = result.unauthorized,
                    notificationsError = result.message
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onSessionExpired: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    GreywireSubScreen(title = "Settings", onBack = onBack, testTag = "settings-screen") {
        Spacer(Modifier.height(16.dp))
        if (uiState.isLoading) {
            LoadingFill()
            return@GreywireSubScreen
        }
        uiState.loadError?.let {
            ErrorText(it, Modifier.testTag("settings-error"))
            Spacer(Modifier.height(16.dp))
        }
        uiState.notifications?.let { settings ->
            NotificationsSection(settings, uiState.notificationsError, viewModel::updateNotifications)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NotificationsSection(
    settings: NotificationSettings,
    error: String?,
    onChange: ((NotificationSettings) -> NotificationSettings) -> Unit
) {
    Eyebrow("Notifications")
    GreywireCard(modifier = Modifier.testTag("notification-settings")) {
        SettingSwitch(
            title = "Allow notifications",
            checked = settings.notificationsEnabled,
            testTag = "notifications-enabled-switch",
            onCheckedChange = { on -> onChange { it.copy(notificationsEnabled = on) } }
        )
        ListDivider()
        // The others only count while notifications are allowed, as on the web
        val enabled = settings.notificationsEnabled
        SettingSwitch(
            title = "New messages in chat",
            checked = settings.notifyChatMessages && enabled,
            enabled = enabled,
            testTag = "notify-chat-switch",
            onCheckedChange = { on -> onChange { it.copy(notifyChatMessages = on) } }
        )
        Spacer(Modifier.height(12.dp))
        SettingSwitch(
            title = "Friend requests",
            checked = settings.notifyFriendRequests && enabled,
            enabled = enabled,
            testTag = "notify-friend-requests-switch",
            onCheckedChange = { on -> onChange { it.copy(notifyFriendRequests = on) } }
        )
        Spacer(Modifier.height(12.dp))
        SettingSwitch(
            title = "Comments on your content",
            checked = settings.notifyComments && enabled,
            enabled = enabled,
            testTag = "notify-comments-switch",
            onCheckedChange = { on -> onChange { it.copy(notifyComments = on) } }
        )
        error?.let {
            Spacer(Modifier.height(12.dp))
            ErrorText(it, Modifier.testTag("notifications-error"))
        }
    }
}

/** A setting's title (and optional description) with a switch on the right. */
@Composable
internal fun SettingSwitch(
    title: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    description: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) GreywireText else GreywireMuted
            )
            description?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = greywireSwitchColors(),
            modifier = Modifier.testTag(testTag)
        )
    }
}
