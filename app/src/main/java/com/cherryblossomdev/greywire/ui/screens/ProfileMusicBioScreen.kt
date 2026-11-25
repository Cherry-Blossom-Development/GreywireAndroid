package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.ProfileRepository
import com.cherryblossomdev.greywire.network.MusicianProfile
import com.cherryblossomdev.greywire.network.MusicianUpdate
import com.cherryblossomdev.greywire.ui.components.BioDraft
import com.cherryblossomdev.greywire.ui.components.ErrorText
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.LoadingFill
import com.cherryblossomdev.greywire.ui.components.RichBioEditor
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Edit the Musician Profile's music bio (roadmap 2.10), as the web's
// MusicianProfile.vue does. `PUT /api/profile/musician` saves the bio with the
// musician-only setting, so that's sent back as loaded.

data class MusicBioUiState(
    val musician: MusicianProfile? = null,
    val bio: BioDraft? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val loadError: String? = null,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class MusicBioViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicBioUiState())
    val uiState: StateFlow<MusicBioUiState> = _uiState

    fun load() {
        if (_uiState.value.musician != null || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            _uiState.value = when (val result = repository.getProfile()) {
                is AuthResult.Success -> {
                    // The musician profile is always there with X-Site: greywire; an empty one otherwise
                    val musician = result.data.user.musician ?: MusicianProfile()
                    _uiState.value.copy(musician = musician, bio = BioDraft(musician.bio), isLoading = false)
                }
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    sessionExpired = result.unauthorized,
                    loadError = result.message
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val musician = state.musician ?: return
        val bio = state.bio ?: return
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val update = MusicianUpdate(bio = bio.html().orEmpty(), musicianOnly = musician.musicianOnly)
            _uiState.value = when (val result = repository.updateMusician(update)) {
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
fun ProfileMusicBioScreen(
    viewModel: MusicBioViewModel,
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

    ProfileFormFrame(title = "Music bio", testTag = "music-bio-screen", onBack = if (uiState.isSaving) null else onBack) {
        val bio = uiState.bio
        if (bio == null) {
            if (uiState.isLoading) LoadingFill()
            uiState.loadError?.let { ErrorText(it, Modifier.testTag("music-bio-error")) }
            return@ProfileFormFrame
        }
        Eyebrow("Musician profile")
        Text(
            "Your music bio",
            style = MaterialTheme.typography.headlineMedium,
            color = GreywireText,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "What you play, who you play with, what you're into. Shown on your Musician Profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = GreywireMuted
        )
        Spacer(Modifier.height(24.dp))

        RichBioEditor(draft = bio, label = "Music bio", enabled = !uiState.isSaving, testTag = "music-bio-input")

        uiState.errorMessage?.let { FormError(it, "music-bio-error") }

        Spacer(Modifier.height(24.dp))
        SaveButton(isSaving = uiState.isSaving, onClick = viewModel::save, testTag = "music-bio-save-button")
        Spacer(Modifier.height(40.dp))
    }
}
