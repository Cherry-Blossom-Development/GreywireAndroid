package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.cherryblossomdev.greywire.network.AlternateEmail
import com.cherryblossomdev.greywire.network.MessageOnlyResponse
import com.cherryblossomdev.greywire.ui.components.GreywireGhostButton
import com.cherryblossomdev.greywire.ui.components.StatusPill
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireSignal
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
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
// Profile > Settings page: notification preferences and the alternate email.

data class SettingsUiState(
    val notifications: NotificationSettings? = null,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val notificationsError: String? = null,
    val altEmail: AlternateEmail? = null,
    /** The address form shows when there's no address yet, or after "Change". */
    val altEmailEditing: Boolean = false,
    val altEmailInput: String = "",
    /** Saving, resending or removing. */
    val altEmailBusy: Boolean = false,
    /** What the backend said after sending or resending ("Verification email sent"). */
    val altEmailMessage: String? = null,
    val altEmailError: String? = null,
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
                is AuthResult.Success -> _uiState.value.copy(notifications = result.data)
                is AuthResult.Error -> _uiState.value.copy(sessionExpired = result.unauthorized, loadError = result.message)
            }
            if (!_uiState.value.sessionExpired) loadAlternateEmail()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun loadAlternateEmail() {
        when (val result = repository.getAlternateEmail()) {
            is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                altEmail = result.data,
                altEmailEditing = result.data.address.isNullOrBlank()
            )
            is AuthResult.Error -> _uiState.value = _uiState.value.copy(
                sessionExpired = result.unauthorized,
                altEmailError = result.message
            )
        }
    }

    fun onAltEmailInput(value: String) {
        _uiState.value = _uiState.value.copy(altEmailInput = value)
    }

    fun startEditAltEmail() {
        _uiState.value = _uiState.value.copy(
            altEmailEditing = true,
            altEmailInput = _uiState.value.altEmail?.address.orEmpty(),
            altEmailMessage = null,
            altEmailError = null
        )
    }

    fun cancelEditAltEmail() {
        _uiState.value = _uiState.value.copy(altEmailEditing = false, altEmailError = null)
    }

    /** Sets the typed address, which sends it a confirmation email. */
    fun saveAltEmail() {
        val address = _uiState.value.altEmailInput.trim()
        if (address.isEmpty()) return
        altEmailAction { repository.setAlternateEmail(address) }
    }

    fun resendAltEmail() = altEmailAction(reload = false) { repository.resendAlternateEmail() }

    fun removeAltEmail() = altEmailAction(showMessage = false) { repository.setAlternateEmail("") }

    /** Runs a call, shows its message or error, and reloads the address's state. */
    private fun altEmailAction(
        reload: Boolean = true,
        showMessage: Boolean = true,
        call: suspend () -> AuthResult<MessageOnlyResponse>
    ) {
        if (_uiState.value.altEmailBusy) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(altEmailBusy = true, altEmailMessage = null, altEmailError = null)
            when (val result = call()) {
                is AuthResult.Success -> {
                    if (reload) loadAlternateEmail()
                    _uiState.value = _uiState.value.copy(
                        altEmailBusy = false,
                        altEmailMessage = if (showMessage) result.data.message else null
                    )
                }
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(
                    altEmailBusy = false,
                    sessionExpired = result.unauthorized,
                    altEmailError = result.message
                )
            }
        }
    }

    /** "Also send notices to this address" (only once it's confirmed). */
    fun setAltEmailNotify(enabled: Boolean) {
        val before = _uiState.value.altEmail ?: return
        _uiState.value = _uiState.value.copy(altEmail = before.copy(sendNotices = enabled), altEmailError = null)
        viewModelScope.launch {
            val result = saveLock.withLock { repository.setAlternateEmailNotify(enabled) }
            if (result is AuthResult.Error) {
                _uiState.value = _uiState.value.copy(
                    altEmail = _uiState.value.altEmail?.copy(sendNotices = !enabled),
                    sessionExpired = result.unauthorized,
                    altEmailError = result.message
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
            Spacer(Modifier.height(32.dp))
        }
        AlternateEmailSection(uiState, viewModel)
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

/** The alternate email: the address form, a pending confirmation, or a confirmed address. */
@Composable
private fun AlternateEmailSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val altEmail = uiState.altEmail
    var confirmRemove by remember { mutableStateOf(false) }

    if (confirmRemove) {
        ConfirmDialog(
            title = "Remove alternate email?",
            text = "Account notices will go to your main email only.",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            onConfirm = {
                confirmRemove = false
                viewModel.removeAltEmail()
            },
            onDismiss = { confirmRemove = false }
        )
    }

    Eyebrow("Alternate email")
    GreywireCard(modifier = Modifier.testTag("alternate-email")) {
        Text(
            "Add a second address so account notices can also reach you there.",
            style = MaterialTheme.typography.bodySmall,
            color = GreywireMuted
        )
        Spacer(Modifier.height(16.dp))
        val address = altEmail?.address
        when {
            altEmail == null -> Unit // Couldn't load; the error shows below
            uiState.altEmailEditing || address.isNullOrBlank() -> {
                ProfileTextField(
                    value = uiState.altEmailInput,
                    onValueChange = viewModel::onAltEmailInput,
                    label = "Alternate email address",
                    enabled = !uiState.altEmailBusy,
                    testTag = "alt-email-input",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
                )
                Spacer(Modifier.height(12.dp))
                SaveButton(
                    isSaving = uiState.altEmailBusy,
                    onClick = viewModel::saveAltEmail,
                    testTag = "alt-email-save-button",
                    label = "Send confirmation email"
                )
                if (!address.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    GreywireGhostButton(
                        onClick = viewModel::cancelEditAltEmail,
                        modifier = Modifier.fillMaxWidth().testTag("alt-email-cancel-button")
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        if (altEmail.verified) "Confirmed" else "Pending",
                        dotColor = if (altEmail.verified) GreywireSignal else GreywireCopper
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        address,
                        style = MonoSmallStyle,
                        color = GreywireText,
                        modifier = Modifier.weight(1f).testTag("alt-email-address")
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (altEmail.verified) {
                    SettingSwitch(
                        title = "Also send notices to this address",
                        checked = altEmail.sendNotices,
                        testTag = "alt-email-notify-switch",
                        onCheckedChange = viewModel::setAltEmailNotify
                    )
                } else {
                    Text(
                        "Check that inbox for a confirmation link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreywireMuted
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!altEmail.verified) {
                        GreywireGhostButton(
                            onClick = viewModel::resendAltEmail,
                            enabled = !uiState.altEmailBusy,
                            modifier = Modifier.testTag("alt-email-resend-button")
                        ) {
                            Text("Resend", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    GreywireGhostButton(
                        onClick = viewModel::startEditAltEmail,
                        enabled = !uiState.altEmailBusy,
                        modifier = Modifier.testTag("alt-email-change-button")
                    ) {
                        Text("Change", style = MaterialTheme.typography.labelMedium)
                    }
                    GreywireGhostButton(
                        onClick = { confirmRemove = true },
                        enabled = !uiState.altEmailBusy,
                        modifier = Modifier.testTag("alt-email-remove-button")
                    ) {
                        Text("Remove", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        uiState.altEmailMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = GreywireSignal,
                modifier = Modifier.testTag("alt-email-message")
            )
        }
        uiState.altEmailError?.let {
            Spacer(Modifier.height(12.dp))
            ErrorText(it, Modifier.testTag("alt-email-error"))
        }
    }
}
