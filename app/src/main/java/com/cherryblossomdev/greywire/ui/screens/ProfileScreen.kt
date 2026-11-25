package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.cherryblossomdev.greywire.network.Profile
import com.cherryblossomdev.greywire.network.ProfileBand
import com.cherryblossomdev.greywire.network.ProfileInstrument
import com.cherryblossomdev.greywire.ui.components.ErrorText
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireGhostButton
import com.cherryblossomdev.greywire.ui.components.GreywireTabFrame
import com.cherryblossomdev.greywire.ui.components.HtmlText
import com.cherryblossomdev.greywire.ui.components.LoadingFill
import com.cherryblossomdev.greywire.ui.components.ProfileAvatar
import com.cherryblossomdev.greywire.ui.components.StatusPill
import com.cherryblossomdev.greywire.ui.components.countLabel
import com.cherryblossomdev.greywire.ui.components.isBlankHtml
import com.cherryblossomdev.greywire.ui.components.parseBackendDate
import com.cherryblossomdev.greywire.ui.components.readableWidth
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSignal
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// The Profile tab (roadmap 2.9 and 2.10): the user's own profile as the web
// community's Profile page shows it on Greywire: photo, name, About, the
// Musician Profile (music bio, instruments, bands) and Details. Greywire
// leaves out Prosaurus's work history. "Edit profile" opens ProfileEditScreen.

data class ProfileUiState(
    val profile: Profile? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            _uiState.value = when (val result = repository.getProfile()) {
                is AuthResult.Success -> _uiState.value.copy(profile = result.data.user, isLoading = false)
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    sessionExpired = result.unauthorized,
                    errorMessage = result.message
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    GreywireTabFrame(title = "Profile", testTag = "profile-screen") {
        val profile = uiState.profile
        if (profile == null && uiState.isLoading) {
            LoadingFill()
            return@GreywireTabFrame
        }
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::load,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .readableWidth()
                    .padding(16.dp)
            ) {
                uiState.errorMessage?.let {
                    ErrorText(it, Modifier.testTag("profile-error"))
                    Spacer(Modifier.height(16.dp))
                }
                if (profile != null) {
                    ProfileHeader(profile)
                    Spacer(Modifier.height(16.dp))
                    GreywireGhostButton(onClick = onEditProfile, modifier = Modifier.testTag("edit-profile-button")) {
                        Text("Edit profile", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(32.dp))
                    AboutSection(profile)
                    profile.musician?.let { MusicianSections(it) }
                    DetailsSection(profile)
                }
                Spacer(Modifier.height(32.dp))
                GreywireGhostButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().testTag("logout-button")) {
                    Text("Sign out", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Photo, name, @handle, member since and friend count (the web's `.profile-header`). */
@Composable
private fun ProfileHeader(profile: Profile) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("profile-header")) {
        ProfileAvatar(profile.photoPath, profile.displayName, size = 80.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                profile.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = GreywireText,
                modifier = Modifier.semantics { heading() }.testTag("profile-name")
            )
            Text("@${profile.handle}", style = MonoSmallStyle, color = GreywireMuted, modifier = Modifier.testTag("profile-handle"))
            Spacer(Modifier.height(6.dp))
            val since = profile.createdAt?.let(::memberSince)
            Text(
                listOfNotNull(since?.let { "Member since $it" }, countLabel(profile.friendCount, "friend")).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = GreywireMuted
            )
        }
    }
}

@Composable
private fun AboutSection(profile: Profile) {
    Eyebrow("About")
    GreywireCard(modifier = Modifier.testTag("profile-about")) {
        if (profile.musician?.musicianOnly == true) {
            Text(
                "Hidden from others on Greywire while you show only your Musician Profile.",
                style = MaterialTheme.typography.bodySmall,
                color = GreywireMuted
            )
            Spacer(Modifier.height(12.dp))
        }
        BioText(profile.bio, empty = "No bio yet.")
    }
    Spacer(Modifier.height(32.dp))
}

/** The Musician Profile (Greywire only): music bio, instruments and bands. */
@Composable
private fun MusicianSections(musician: MusicianProfile) {
    Eyebrow("Musician profile")
    GreywireCard(modifier = Modifier.testTag("musician-profile")) {
        if (musician.musicianOnly) {
            StatusPill("Musician profile only", dotColor = GreywireSignal)
            Spacer(Modifier.height(12.dp))
        }
        BioText(musician.bio, empty = "No music bio yet.")
    }
    Spacer(Modifier.height(24.dp))

    Eyebrow("Instruments")
    GreywireCard(modifier = Modifier.testTag("profile-instruments")) {
        if (musician.instruments.isEmpty()) {
            EmptyLine("No instruments yet.")
        } else {
            musician.instruments.forEachIndexed { index, instrument ->
                if (index > 0) ListDivider()
                InstrumentRow(instrument)
            }
        }
    }
    Spacer(Modifier.height(24.dp))

    Eyebrow("Bands")
    GreywireCard(modifier = Modifier.testTag("profile-bands")) {
        if (musician.bands.isEmpty()) {
            EmptyLine("No bands yet.")
        } else {
            musician.bands.forEachIndexed { index, band ->
                if (index > 0) ListDivider()
                BandRow(band)
            }
        }
    }
    Spacer(Modifier.height(32.dp))
}

@Composable
private fun InstrumentRow(instrument: ProfileInstrument) {
    Column(modifier = Modifier.fillMaxWidth().testTag("profile-instrument-${instrument.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(instrument.name, style = MaterialTheme.typography.titleMedium, color = GreywireText)
            if (instrument.isPrimary) StatusPill("Main")
        }
        instrument.details.takeIf { it.isNotEmpty() }?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
        }
    }
}

@Composable
private fun BandRow(band: ProfileBand) {
    Column(modifier = Modifier.fillMaxWidth().testTag("profile-band-${band.id}")) {
        Text(band.bandName, style = MaterialTheme.typography.titleMedium, color = GreywireText)
        val details = listOfNotNull(band.role, band.years.takeIf { it.isNotEmpty() }).joinToString(" · ")
        if (details.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(details, style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
        }
    }
}

@Composable
private fun DetailsSection(profile: Profile) {
    Eyebrow("Details")
    GreywireCard(modifier = Modifier.testTag("profile-details")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            profile.email?.let { DetailLine("Email", it) }
            DetailLine("First name", profile.firstName?.takeIf { it.isNotBlank() } ?: "Not set")
            DetailLine("Last name", profile.lastName?.takeIf { it.isNotBlank() } ?: "Not set")
        }
    }
}

@Composable
private fun BioText(html: String?, empty: String) {
    if (isBlankHtml(html)) EmptyLine(empty) else HtmlText(html!!)
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
}

@Composable
private fun ListDivider() {
    HorizontalDivider(color = GreywireLine, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
        Spacer(Modifier.width(16.dp))
        Text(value, style = MonoSmallStyle, color = GreywireText)
    }
}

/** "November 2026" from the account's creation timestamp. */
private fun memberSince(createdAt: String): String? =
    parseBackendDate(createdAt)?.let { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(it) }
