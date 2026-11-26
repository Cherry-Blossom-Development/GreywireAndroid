package com.cherryblossomdev.greywire.ui.screens

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.cherryblossomdev.greywire.data.resizePhotoForUpload
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
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
import androidx.compose.material3.Switch
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
import com.cherryblossomdev.greywire.network.MusicianUpdate
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
import com.cherryblossomdev.greywire.ui.components.popupTestTags
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
    /** A pull to refresh (other reloads happen quietly behind the shown profile). */
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    /** Saving the musician-only switch. */
    val isSavingMusician: Boolean = false,
    val musicianError: String? = null,
    /** Uploading or removing the photo. */
    val isSavingPhoto: Boolean = false,
    val photoError: String? = null,
    val sessionExpired: Boolean = false
)

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    // Starts from the last profile loaded, so coming back from an edit screen
    // shows it at once (keeping the scroll position) while it reloads
    private val _uiState = MutableStateFlow(ProfileUiState(profile = repository.lastProfile))
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun load(refreshing: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isRefreshing = refreshing, errorMessage = null)
            _uiState.value = when (val result = repository.getProfile()) {
                is AuthResult.Success -> _uiState.value.copy(profile = result.data.user, isLoading = false, isRefreshing = false)
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    sessionExpired = result.unauthorized,
                    errorMessage = result.message
                )
            }
        }
    }

    /** Shrinks the picked image (see [resizePhotoForUpload]) and uploads it as the profile photo. */
    fun uploadPhoto(resolver: ContentResolver, uri: Uri) {
        if (_uiState.value.isSavingPhoto) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingPhoto = true, photoError = null)
            val jpeg = try {
                withContext(Dispatchers.IO) { resizePhotoForUpload(resolver, uri) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSavingPhoto = false, photoError = "That file isn't an image the app can read.")
                return@launch
            }
            _uiState.value = when (val result = repository.uploadPhoto(jpeg)) {
                is AuthResult.Success -> _uiState.value.copy(
                    profile = _uiState.value.profile?.copy(photoPath = result.data.photoPath),
                    isSavingPhoto = false
                )
                is AuthResult.Error -> _uiState.value.copy(
                    isSavingPhoto = false,
                    sessionExpired = result.unauthorized,
                    photoError = result.message
                )
            }
        }
    }

    fun removePhoto() {
        if (_uiState.value.isSavingPhoto) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingPhoto = true, photoError = null)
            _uiState.value = when (val result = repository.removePhoto()) {
                is AuthResult.Success -> _uiState.value.copy(
                    profile = _uiState.value.profile?.copy(photoPath = null),
                    isSavingPhoto = false
                )
                is AuthResult.Error -> _uiState.value.copy(
                    isSavingPhoto = false,
                    sessionExpired = result.unauthorized,
                    photoError = result.message
                )
            }
        }
    }

    /** "Show only my Musician Profile": hides the About section from others on Greywire. */
    fun setMusicianOnly(musicianOnly: Boolean) {
        val musician = _uiState.value.profile?.musician ?: return
        if (_uiState.value.isSavingMusician) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingMusician = true, musicianError = null)
            val update = MusicianUpdate(bio = musician.bio.orEmpty(), musicianOnly = musicianOnly)
            _uiState.value = when (val result = repository.updateMusician(update)) {
                is AuthResult.Success -> _uiState.value.copy(
                    profile = _uiState.value.profile?.copy(musician = result.data.musician),
                    isSavingMusician = false
                )
                is AuthResult.Error -> _uiState.value.copy(
                    isSavingMusician = false,
                    sessionExpired = result.unauthorized,
                    musicianError = result.message
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
    onEditMusicBio: () -> Unit,
    onEditInstrument: (Int?) -> Unit,
    onEditBand: (Int?) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.load(refreshing = true) },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        GreywireGhostButton(onClick = onEditProfile, modifier = Modifier.testTag("edit-profile-button")) {
                            Text("Edit profile", style = MaterialTheme.typography.labelMedium)
                        }
                        PhotoButton(
                            hasPhoto = !profile.photoPath.isNullOrBlank(),
                            isSaving = uiState.isSavingPhoto,
                            onPick = { viewModel.uploadPhoto(context.contentResolver, it) },
                            onRemove = viewModel::removePhoto
                        )
                    }
                    uiState.photoError?.let {
                        Spacer(Modifier.height(12.dp))
                        ErrorText(it, Modifier.testTag("photo-error"))
                    }
                    Spacer(Modifier.height(32.dp))
                    AboutSection(profile)
                    profile.musician?.let {
                        MusicianSections(
                            musician = it,
                            isSaving = uiState.isSavingMusician,
                            error = uiState.musicianError,
                            onMusicianOnlyChange = viewModel::setMusicianOnly,
                            onEditMusicBio = onEditMusicBio,
                            onEditInstrument = onEditInstrument,
                            onEditBand = onEditBand
                        )
                    }
                    DetailsSection(profile)
                }
                Spacer(Modifier.height(32.dp))
                GreywireGhostButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().testTag("settings-button")) {
                    Text("Settings", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(12.dp))
                GreywireGhostButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().testTag("logout-button")) {
                    Text("Sign out", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * "Photo": a menu with "Choose a new photo" (Android's photo picker, which
 * needs no permission) and, when there is one, "Remove photo" (after confirming).
 */
@Composable
private fun PhotoButton(hasPhoto: Boolean, isSaving: Boolean, onPick: (Uri) -> Unit, onRemove: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPick)
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "Remove your photo?",
            text = "Your profile will show your initial instead.",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            onConfirm = {
                confirmRemove = false
                onRemove()
            },
            onDismiss = { confirmRemove = false }
        )
    }

    Box {
        GreywireGhostButton(onClick = { menuOpen = true }, enabled = !isSaving, modifier = Modifier.testTag("photo-button")) {
            if (isSaving) {
                CircularProgressIndicator(Modifier.size(16.dp), color = GreywireCopper, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Saving photo...", style = MaterialTheme.typography.labelMedium)
            } else {
                Text(if (hasPhoto) "Change photo" else "Add photo", style = MaterialTheme.typography.labelMedium)
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = GreywireSurface,
            modifier = Modifier.popupTestTags()
        ) {
            DropdownMenuItem(
                text = { Text("Choose a new photo", color = GreywireText) },
                onClick = {
                    menuOpen = false
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.testTag("choose-photo-item")
            )
            if (hasPhoto) {
                DropdownMenuItem(
                    text = { Text("Remove photo", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        confirmRemove = true
                    },
                    modifier = Modifier.testTag("remove-photo-item")
                )
            }
        }
    }
}

/** Photo, name, @handle, member since and friend count (the web's `.profile-header`). */
@Composable
internal fun ProfileHeader(profile: Profile) {
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
private fun MusicianSections(
    musician: MusicianProfile,
    isSaving: Boolean,
    error: String?,
    onMusicianOnlyChange: (Boolean) -> Unit,
    onEditMusicBio: () -> Unit,
    /** A user_instruments row id to edit, or null to add one. */
    onEditInstrument: (Int?) -> Unit,
    /** A user_band_history row id to edit, or null to add one. */
    onEditBand: (Int?) -> Unit
) {
    Eyebrow("Musician profile")
    GreywireCard(modifier = Modifier.testTag("musician-profile")) {
        if (musician.musicianOnly) {
            StatusPill("Musician profile only", dotColor = GreywireSignal)
            Spacer(Modifier.height(12.dp))
        }
        BioText(musician.bio, empty = "No music bio yet.")
        Spacer(Modifier.height(16.dp))
        GreywireGhostButton(onClick = onEditMusicBio, modifier = Modifier.testTag("edit-music-bio-button")) {
            Text("Edit music bio", style = MaterialTheme.typography.labelMedium)
        }
        ListDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show only my Musician Profile", style = MaterialTheme.typography.titleMedium, color = GreywireText)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Others on Greywire see your music bio, instruments and bands, but not your About section.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GreywireMuted
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = musician.musicianOnly,
                onCheckedChange = onMusicianOnlyChange,
                enabled = !isSaving,
                colors = greywireSwitchColors(),
                modifier = Modifier.testTag("musician-only-switch")
            )
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            ErrorText(it, Modifier.testTag("musician-error"))
        }
    }
    Spacer(Modifier.height(24.dp))

    Eyebrow("Instruments")
    GreywireCard(modifier = Modifier.testTag("profile-instruments")) {
        if (musician.instruments.isEmpty()) {
            EmptyLine("No instruments yet.")
        } else {
            musician.instruments.forEachIndexed { index, instrument ->
                if (index > 0) ListDivider()
                InstrumentRow(instrument, onClick = { onEditInstrument(instrument.id) })
            }
        }
        Spacer(Modifier.height(16.dp))
        GreywireGhostButton(
            onClick = { onEditInstrument(null) },
            modifier = Modifier.fillMaxWidth().testTag("add-instrument-button")
        ) {
            Text("Add instrument", style = MaterialTheme.typography.labelMedium)
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
                BandRow(band, onClick = { onEditBand(band.id) })
            }
        }
        Spacer(Modifier.height(16.dp))
        GreywireGhostButton(
            onClick = { onEditBand(null) },
            modifier = Modifier.fillMaxWidth().testTag("add-band-button")
        ) {
            Text("Add band", style = MaterialTheme.typography.labelMedium)
        }
    }
    Spacer(Modifier.height(32.dp))
}

@Composable
private fun InstrumentRow(instrument: ProfileInstrument, onClick: () -> Unit) {
    EditableRow(onClick = onClick, editLabel = "Edit ${instrument.name}", testTag = "profile-instrument-${instrument.id}") {
        InstrumentInfo(instrument)
    }
}

/** An instrument's name, "Main" pill and details (shared with other members' profiles). */
@Composable
internal fun InstrumentInfo(instrument: ProfileInstrument) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(instrument.name, style = MaterialTheme.typography.titleMedium, color = GreywireText)
        if (instrument.isPrimary) StatusPill("Main")
    }
    instrument.details.takeIf { it.isNotEmpty() }?.let {
        Spacer(Modifier.height(2.dp))
        Text(it, style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
    }
}

/** A list row that opens its editor when tapped, with a pencil at the end. */
@Composable
private fun EditableRow(onClick: () -> Unit, editLabel: String, testTag: String, content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = editLabel, onClick = onClick)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.weight(1f)) { content() }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Outlined.Edit, contentDescription = null, tint = GreywireMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BandRow(band: ProfileBand, onClick: () -> Unit) {
    EditableRow(onClick = onClick, editLabel = "Edit ${band.bandName}", testTag = "profile-band-${band.id}") {
        BandInfo(band)
    }
}

/** A band's name, role and years (shared with other members' profiles). */
@Composable
internal fun BandInfo(band: ProfileBand) {
    Text(band.bandName, style = MaterialTheme.typography.titleMedium, color = GreywireText)
    val details = listOfNotNull(band.role, band.years.takeIf { it.isNotEmpty() }).joinToString(" · ")
    if (details.isNotEmpty()) {
        Spacer(Modifier.height(2.dp))
        Text(details, style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
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
internal fun BioText(html: String?, empty: String) {
    if (isBlankHtml(html)) EmptyLine(empty) else HtmlText(html!!)
}

@Composable
internal fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
}

@Composable
internal fun ListDivider() {
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
