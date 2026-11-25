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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.ProfileRepository
import com.cherryblossomdev.greywire.network.Profile
import com.cherryblossomdev.greywire.network.ProfileUpdate
import com.cherryblossomdev.greywire.ui.components.BioDraft
import com.cherryblossomdev.greywire.ui.components.ErrorText
import com.cherryblossomdev.greywire.ui.components.RichBioEditor
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireButton
import com.cherryblossomdev.greywire.ui.components.GreywireTopBar
import com.cherryblossomdev.greywire.ui.components.LoadingFill
import com.cherryblossomdev.greywire.ui.components.greywireTextFieldColors
import com.cherryblossomdev.greywire.ui.components.readableWidth
import com.cherryblossomdev.greywire.ui.theme.GreywireBg
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface2
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Edit profile (roadmap 2.9): first and last name and the About bio, as the
// web's Profile page edits them. `PUT /api/profile` saves the names and both
// bios together; Prosaurus's work bio (hidden on Greywire) is sent back as
// loaded, and so is the bio when it wasn't changed (see BioDraft).

data class ProfileEditUiState(
    val profile: Profile? = null,
    val firstName: String = "",
    val lastName: String = "",
    /** The About bio in the editor; set once the profile loads. */
    val bio: BioDraft? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class ProfileEditViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState

    fun load() {
        if (_uiState.value.profile != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            _uiState.value = when (val result = repository.getProfile()) {
                is AuthResult.Success -> {
                    val profile = result.data.user
                    _uiState.value.copy(
                        profile = profile,
                        firstName = profile.firstName.orEmpty(),
                        lastName = profile.lastName.orEmpty(),
                        bio = BioDraft(profile.bio),
                        isLoading = false
                    )
                }
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    sessionExpired = result.unauthorized,
                    errorMessage = result.message
                )
            }
        }
    }

    fun setFirstName(value: String) { _uiState.value = _uiState.value.copy(firstName = value) }
    fun setLastName(value: String) { _uiState.value = _uiState.value.copy(lastName = value) }

    fun save() {
        val state = _uiState.value
        val profile = state.profile ?: return
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val update = ProfileUpdate(
                firstName = state.firstName.trim(),
                lastName = state.lastName.trim(),
                bio = state.bio?.html() ?: profile.bio,
                workBio = profile.workBio
            )
            _uiState.value = when (val result = repository.updateProfile(update)) {
                is AuthResult.Success -> _uiState.value.copy(isSaving = false, saved = true)
                is AuthResult.Error -> _uiState.value.copy(
                    isSaving = false,
                    sessionExpired = result.unauthorized,
                    errorMessage = result.message
                )
            }
        }
    }
}

@Composable
fun ProfileEditScreen(
    viewModel: ProfileEditViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    ProfileFormFrame(title = "Edit profile", testTag = "profile-edit-screen", onBack = if (uiState.isSaving) null else onBack) {
        if (uiState.profile == null) {
            if (uiState.isLoading) LoadingFill()
            uiState.errorMessage?.let { ErrorText(it, Modifier.testTag("profile-edit-error")) }
            return@ProfileFormFrame
        }
        Eyebrow("Profile")
        Text(
            "Your name and bio",
            style = MaterialTheme.typography.headlineMedium,
            color = GreywireText,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Shown on your profile, in Friends and in Chat. Leave both blank to show your @handle instead.",
            style = MaterialTheme.typography.bodyMedium,
            color = GreywireMuted
        )
        Spacer(Modifier.height(24.dp))

        ProfileTextField(
            value = uiState.firstName,
            onValueChange = viewModel::setFirstName,
            label = "First name",
            enabled = !uiState.isSaving,
            testTag = "edit-first-name-input"
        )
        Spacer(Modifier.height(16.dp))
        ProfileTextField(
            value = uiState.lastName,
            onValueChange = viewModel::setLastName,
            label = "Last name",
            enabled = !uiState.isSaving,
            testTag = "edit-last-name-input",
            imeAction = ImeAction.Done
        )

        uiState.bio?.let { bio ->
            Spacer(Modifier.height(32.dp))
            Eyebrow("About")
            Text(
                "Your bio, shown in the About section of your profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = GreywireMuted
            )
            Spacer(Modifier.height(16.dp))
            RichBioEditor(draft = bio, label = "Bio", enabled = !uiState.isSaving, testTag = "edit-bio-input")
        }

        uiState.errorMessage?.let { FormError(it, "profile-edit-error") }

        Spacer(Modifier.height(24.dp))
        SaveButton(isSaving = uiState.isSaving, onClick = viewModel::save, testTag = "profile-save-button")
        Spacer(Modifier.height(40.dp))
    }
}

/**
 * A full-screen profile form: top bar, then scrolling content capped at
 * [maxWidth], kept above the keyboard. [onBack] is null while saving.
 */
@Composable
internal fun ProfileFormFrame(
    title: String,
    testTag: String,
    onBack: (() -> Unit)?,
    maxWidth: Dp = 520.dp,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding().testTag(testTag)) {
        GreywireTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .readableWidth(maxWidth)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
internal fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = imeAction)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = greywireTextFieldColors(),
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().testTag(testTag)
    )
}

@Composable
internal fun FormError(message: String, testTag: String) {
    Spacer(Modifier.height(16.dp))
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag(testTag).semantics { liveRegion = LiveRegionMode.Polite }
    )
}

/** A Switch in the site's colors: copper when on. */
@Composable
internal fun greywireSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = GreywireBg,
    checkedTrackColor = GreywireCopper,
    uncheckedThumbColor = GreywireMuted,
    uncheckedTrackColor = GreywireSurface2,
    uncheckedBorderColor = GreywireLine
)

@Composable
internal fun SaveButton(isSaving: Boolean, onClick: () -> Unit, testTag: String, label: String = "Save") {
    GreywireButton(onClick = onClick, enabled = !isSaving, modifier = Modifier.fillMaxWidth().testTag(testTag)) {
        if (isSaving) {
            CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Saving...")
        } else {
            Text(label)
        }
    }
}
