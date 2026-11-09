package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.BuildConfig
import com.cherryblossomdev.greywire.data.AuthRepository
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireBrand
import com.cherryblossomdev.greywire.ui.components.GreywireButton
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val handle: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val userId: Int = 0
)

// Same flow as the Prosaurus LoginViewModel: log in, then fetch /me for the user ID.
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun updateHandle(handle: String) {
        _uiState.value = _uiState.value.copy(handle = handle, errorMessage = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return
        if (state.handle.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Enter your username and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = authRepository.login(state.handle, state.password)) {
                is AuthResult.Success -> {
                    val userId = (authRepository.getMe() as? AuthResult.Success)?.data?.userId ?: 0
                    // Login worked even if /me failed; carry on either way
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true, userId = userId)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (userId: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess(uiState.userId)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GreywireCopper,
        unfocusedBorderColor = GreywireLine,
        focusedLabelColor = GreywireCopper,
        unfocusedLabelColor = GreywireMuted,
        cursorColor = GreywireCopper,
        focusedTextColor = GreywireText,
        unfocusedTextColor = GreywireText,
        focusedContainerColor = GreywireSurface,
        unfocusedContainerColor = GreywireSurface,
        disabledContainerColor = GreywireSurface
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                GreywireBrand(logoSize = 44, textSize = 22)

                Spacer(Modifier.height(40.dp))

                Eyebrow("Linework series")
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.displaySmall,
                    color = GreywireText,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Use your Greywire account. It's the same account you use on Prosaurus.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GreywireMuted
                )

                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = uiState.handle,
                    onValueChange = viewModel::updateHandle,
                    label = { Text("Username or email") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("username-input"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    enabled = !uiState.isLoading
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = GreywireMuted
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("password-input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.login()
                    }),
                    enabled = !uiState.isLoading
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
                    onClick = viewModel::login,
                    modifier = Modifier.fillMaxWidth().testTag("login-button"),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign in")
                    }
                }

                // Inside the scrolling column (not pinned to the bottom) so the
                // keyboard can't push it over the button
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MonoSmallStyle,
                    color = GreywireMuted
                )
            }
        }
    }
}
