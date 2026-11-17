package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthRepository
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireButton
import com.cherryblossomdev.greywire.ui.components.GreywireTopBar
import com.cherryblossomdev.greywire.ui.components.greywireTextFieldColors
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Create an account (POST /api/auth/signup). Same fields and checks as the
// Prosaurus SignupScreen; on success the user is signed in and goes on to the
// EULA step, then Home, where they can claim a pedal.

data class SignupUiState(
    val handle: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val userId: Int = 0
)

class SignupViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState

    fun updateHandle(value: String) = update { copy(handle = value) }
    fun updateFirstName(value: String) = update { copy(firstName = value) }
    fun updateLastName(value: String) = update { copy(lastName = value) }
    fun updateEmail(value: String) = update { copy(email = value) }
    fun updatePassword(value: String) = update { copy(password = value) }
    fun updateConfirmPassword(value: String) = update { copy(confirmPassword = value) }

    private fun update(change: SignupUiState.() -> SignupUiState) {
        _uiState.value = _uiState.value.change().copy(errorMessage = null)
    }

    fun signup() {
        val state = _uiState.value
        if (state.isLoading) return
        val problem = when {
            listOf(state.handle, state.firstName, state.lastName, state.email, state.password).any { it.isBlank() } ->
                "Fill in all the fields"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() ->
                "Enter a valid email address"
            state.password.length < 6 -> "Use a password of at least 6 characters"
            state.password != state.confirmPassword -> "The passwords don't match"
            else -> null
        }
        if (problem != null) {
            _uiState.value = state.copy(errorMessage = problem)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signup(
                handle = state.handle,
                firstName = state.firstName,
                lastName = state.lastName,
                email = state.email,
                password = state.password
            )) {
                is AuthResult.Success -> {
                    // The account exists and is signed in even if /me fails; carry on either way
                    val userId = (authRepository.getMe() as? AuthResult.Success)?.data?.userId ?: 0
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true, userId = userId)
                }
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }
}

@Composable
fun SignupScreen(
    viewModel: SignupViewModel,
    onBack: () -> Unit,
    onSignupSuccess: (userId: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    val fieldColors = greywireTextFieldColors()
    val enabled = !uiState.isLoading

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onSignupSuccess(uiState.userId)
    }

    // Next (not Down), so first name goes on to last name in the same row
    val next = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        GreywireTopBar(title = "Create an account", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().testTag("signup-screen")) {
                Spacer(Modifier.height(32.dp))
                Eyebrow("Greywire Studio")
                Text(
                    text = "Join Greywire",
                    style = MaterialTheme.typography.displaySmall,
                    color = GreywireText,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "One account works on Greywire and Prosaurus. If you already have a Prosaurus account, sign in with it instead.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GreywireMuted
                )
                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = uiState.handle,
                    onValueChange = viewModel::updateHandle,
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
                    keyboardActions = next,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().testTag("signup-username-input")
                )
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.firstName,
                        onValueChange = viewModel::updateFirstName,
                        label = { Text("First name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = next,
                        enabled = enabled,
                        modifier = Modifier.weight(1f).testTag("first-name-input")
                    )
                    OutlinedTextField(
                        value = uiState.lastName,
                        onValueChange = viewModel::updateLastName,
                        label = { Text("Last name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = next,
                        enabled = enabled,
                        modifier = Modifier.weight(1f).testTag("last-name-input")
                    )
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = next,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().testTag("email-input")
                )
                Spacer(Modifier.height(16.dp))

                val passwordTransformation =
                    if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    visualTransformation = passwordTransformation,
                    trailingIcon = {
                        // Out of the keyboard's Next order, so Next goes on to Confirm password
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.focusProperties { canFocus = false }
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide passwords" else "Show passwords",
                                tint = GreywireMuted
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = next,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().testTag("password-input")
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::updateConfirmPassword,
                    label = { Text("Confirm password") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    visualTransformation = passwordTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.signup()
                    }),
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().testTag("confirm-password-input")
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
                        viewModel.signup()
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().testTag("create-account-button")
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create account")
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onBack,
                    enabled = enabled,
                    modifier = Modifier.align(Alignment.CenterHorizontally).testTag("go-to-login-button")
                ) {
                    Text("Already have an account? Sign in", color = GreywireCopper)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
