package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthRepository
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.ui.components.GreywireButton
import com.cherryblossomdev.greywire.ui.components.GreywireGhostButton
import com.cherryblossomdev.greywire.ui.theme.GreywireBg
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireError
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// The EULA accept step. Every signed-in launch passes through here before
// Home: the backend is asked whether this account has accepted the EULA
// (GET /api/eula/status), and if not, the user reads it and taps Accept
// (POST /api/eula/accept) or declines and is signed out. Same flow as the
// Prosaurus app's EulaScreen, and as GreywireiPhone's EulaGateView.
//
// If the status check fails for a reason other than a rejected token, the
// user is let through rather than locked out of the app (as Prosaurus does).

data class EulaGateUiState(
    val isChecking: Boolean = true,
    val accepted: Boolean = false,
    val isAccepting: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class EulaGateViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EulaGateUiState())
    val uiState: StateFlow<EulaGateUiState> = _uiState

    fun check() {
        viewModelScope.launch {
            _uiState.value = when (val result = authRepository.getEulaStatus()) {
                is AuthResult.Success -> EulaGateUiState(isChecking = false, accepted = result.data.accepted)
                is AuthResult.Error -> if (result.unauthorized) {
                    EulaGateUiState(isChecking = false, sessionExpired = true)
                } else {
                    EulaGateUiState(isChecking = false, accepted = true)
                }
            }
        }
    }

    fun accept() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAccepting = true, errorMessage = null)
            _uiState.value = when (val result = authRepository.acceptEula()) {
                is AuthResult.Success -> _uiState.value.copy(isAccepting = false, accepted = true)
                is AuthResult.Error -> _uiState.value.copy(
                    isAccepting = false,
                    sessionExpired = result.unauthorized,
                    errorMessage = "Couldn't save your acceptance. Please try again."
                )
            }
        }
    }
}

@Composable
fun EulaGateScreen(
    viewModel: EulaGateViewModel,
    onAccepted: () -> Unit,
    onDecline: () -> Unit,
    onSessionExpired: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.check() }
    LaunchedEffect(uiState.accepted, uiState.isChecking) {
        if (!uiState.isChecking && uiState.accepted) onAccepted()
    }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    if (uiState.isChecking || uiState.accepted || uiState.sessionExpired) {
        Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = GreywireCopper, strokeWidth = 2.dp)
        }
        return
    }

    EulaScreen(
        onBack = null,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        footer = {
            AcceptFooter(
                isAccepting = uiState.isAccepting,
                errorMessage = uiState.errorMessage,
                onAccept = viewModel::accept,
                onDecline = onDecline
            )
        }
    )
}

@Composable
private fun AcceptFooter(
    isAccepting: Boolean,
    errorMessage: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(GreywireBg)) {
        HorizontalDivider(color = GreywireLine, thickness = 1.dp)
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Please read and accept the End User License Agreement to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = GreywireMuted,
                textAlign = TextAlign.Center
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = GreywireError,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("eula-error")
                )
            }
            GreywireButton(
                onClick = onAccept,
                enabled = !isAccepting,
                modifier = Modifier.fillMaxWidth().testTag("eula-accept-button")
            ) {
                if (isAccepting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Accept These Terms", style = MaterialTheme.typography.labelLarge)
                }
            }
            GreywireGhostButton(
                onClick = onDecline,
                enabled = !isAccepting,
                modifier = Modifier.fillMaxWidth().testTag("eula-decline-button")
            ) {
                Text("Decline and sign out", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
