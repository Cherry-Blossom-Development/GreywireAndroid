package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.ProfileRepository
import com.cherryblossomdev.greywire.network.MusicianProfile
import com.cherryblossomdev.greywire.network.Profile
import com.cherryblossomdev.greywire.ui.components.ErrorText
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireSubScreen
import com.cherryblossomdev.greywire.ui.components.LoadingFill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Another member's profile (roadmap 2.9), as the web's PublicProfilePage shows
// it on Greywire: photo, name, @handle, member since and friend count; About
// (left out when they show only their Musician Profile); and the Musician
// Profile. Read only. Opened from the Friends tab.

data class UserProfileUiState(
    val profile: Profile? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class UserProfileViewModel(private val repository: ProfileRepository, private val handle: String) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = when (val result = repository.getUserProfile(handle)) {
                is AuthResult.Success -> UserProfileUiState(profile = result.data.user, isLoading = false)
                is AuthResult.Error -> UserProfileUiState(
                    isLoading = false,
                    sessionExpired = result.unauthorized,
                    errorMessage = result.message
                )
            }
        }
    }
}

@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel, handle: String, onBack: () -> Unit, onSessionExpired: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    GreywireSubScreen(title = "@$handle", onBack = onBack, testTag = "user-profile-screen", maxWidth = 632.dp) {
        Spacer(Modifier.height(16.dp))
        val profile = uiState.profile
        when {
            uiState.isLoading -> LoadingFill()
            profile == null -> ErrorText(uiState.errorMessage ?: "Couldn't load this profile", Modifier.testTag("user-profile-error"))
            else -> {
                ProfileHeader(profile)
                Spacer(Modifier.height(32.dp))
                // Hidden when they show only their Musician Profile (the backend sends no bio then)
                if (profile.musician?.musicianOnly != true) {
                    Eyebrow("About")
                    GreywireCard(modifier = Modifier.testTag("profile-about")) {
                        BioText(profile.bio, empty = "No bio yet.")
                    }
                    Spacer(Modifier.height(32.dp))
                }
                profile.musician?.let { MusicianSectionsReadOnly(it) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** The Musician Profile without the edit controls: music bio, instruments and bands. */
@Composable
private fun MusicianSectionsReadOnly(musician: MusicianProfile) {
    Eyebrow("Musician profile")
    GreywireCard(modifier = Modifier.testTag("musician-profile")) {
        BioText(musician.bio, empty = "No music bio yet.")
    }
    Spacer(Modifier.height(24.dp))

    Eyebrow("Instruments")
    GreywireCard(modifier = Modifier.testTag("profile-instruments")) {
        if (musician.instruments.isEmpty()) {
            EmptyLine("No instruments listed.")
        } else {
            musician.instruments.forEachIndexed { index, instrument ->
                if (index > 0) ListDivider()
                Column(Modifier.testTag("profile-instrument-${instrument.id}")) {
                    InstrumentInfo(instrument)
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))

    Eyebrow("Bands")
    GreywireCard(modifier = Modifier.testTag("profile-bands")) {
        if (musician.bands.isEmpty()) {
            EmptyLine("No bands listed.")
        } else {
            musician.bands.forEachIndexed { index, band ->
                if (index > 0) ListDivider()
                Column(Modifier.testTag("profile-band-${band.id}")) {
                    BandInfo(band)
                }
            }
        }
    }
}
