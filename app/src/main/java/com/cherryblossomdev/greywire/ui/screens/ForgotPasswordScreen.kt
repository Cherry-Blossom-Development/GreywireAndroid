package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthRepository
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireButton
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireTopBar
import com.cherryblossomdev.greywire.ui.components.greywireTextFieldColors
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Request a password reset email (POST /api/auth/forgot-password). The email's
// link opens the Greywire website, where the new password is set; the app
// doesn't handle the link yet (decided 2026-11-18, see ROADMAP.md).

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSent: Boolean = false
)

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun send() {
        val state = _uiState.value
        if (state.isLoading) return
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()) {
            _uiState.value = state.copy(errorMessage = "Enter a valid email address")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.forgotPassword(state.email)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, isSent = true)
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        GreywireTopBar(title = "Forgot password", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().testTag("forgot-password-screen")) {
                Spacer(Modifier.height(32.dp))
                Eyebrow("Your account")
                Text(
                    text = "Reset your password",
                    style = MaterialTheme.typography.displaySmall,
                    color = GreywireText,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(8.dp))

                if (uiState.isSent) {
                    Text(
                        text = "Check your inbox for the reset link. It opens the Greywire website, where you can choose a new password, then sign in here with it.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GreywireMuted
                    )
                    Spacer(Modifier.height(24.dp))
                    GreywireCard(modifier = Modifier.testTag("reset-sent-message")) {
                        Text(
                            // The backend says this whether or not the email is registered
                            text = "If ${uiState.email.trim()} is registered, a reset link is on its way. The link works for one hour.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GreywireText
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    GreywireButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().testTag("back-to-login-button")
                    ) {
                        Text("Back to sign in")
                    }
                } else {
                    Text(
                        text = "Enter the email address on your account, and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GreywireMuted
                    )
                    Spacer(Modifier.height(28.dp))
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::updateEmail,
                        label = { Text("Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = greywireTextFieldColors(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Send,
                            autoCorrectEnabled = false
                        ),
                        keyboardActions = KeyboardActions(onSend = {
                            focusManager.clearFocus()
                            viewModel.send()
                        }),
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().testTag("email-input")
                    )

                    uiState.errorMessage?.let { error ->
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .testTag("error-message")
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }

                    Spacer(Modifier.height(28.dp))
                    GreywireButton(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.send()
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().testTag("send-reset-button")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send reset link")
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
