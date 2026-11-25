package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.ProfileRepository
import com.cherryblossomdev.greywire.network.BandUpdate
import com.cherryblossomdev.greywire.network.MusicianResponse
import com.cherryblossomdev.greywire.network.ProfileBand
import com.cherryblossomdev.greywire.ui.components.ErrorText
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireGhostButton
import com.cherryblossomdev.greywire.ui.components.LoadingFill
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

// Add or edit a band in the Musician Profile's band history (roadmap 2.10),
// as the web's MusicianProfile.vue does: name, role, start and end year, and
// "I'm in this band now" (which clears the end year). Editing offers Remove.
// These are free text, not Greywire bands.

data class ProfileBandUiState(
    /** Null when adding. */
    val existing: ProfileBand? = null,
    val bandName: String = "",
    val role: String = "",
    val startYear: String = "",
    val endYear: String = "",
    val isCurrent: Boolean = false,
    val isLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val done: Boolean = false,
    val loadError: String? = null,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
) {
    val isNew: Boolean get() = existing == null
}

/** [rowId] is the user_band_history row to edit, or null to add one. */
class ProfileBandViewModel(
    private val repository: ProfileRepository,
    private val rowId: Int?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileBandUiState(isLoaded = rowId == null))
    val uiState: StateFlow<ProfileBandUiState> = _uiState

    fun load() {
        if (_uiState.value.isLoaded || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            _uiState.value = when (val result = repository.getProfile()) {
                is AuthResult.Success -> {
                    val band = result.data.user.musician?.bands?.firstOrNull { it.id == rowId }
                    if (band == null) {
                        _uiState.value.copy(isLoading = false, loadError = "This band is no longer on your profile.")
                    } else {
                        _uiState.value.copy(
                            existing = band,
                            bandName = band.bandName,
                            role = band.role.orEmpty(),
                            startYear = band.startYear?.toString().orEmpty(),
                            endYear = band.endYear?.toString().orEmpty(),
                            isCurrent = band.isCurrent,
                            isLoading = false,
                            isLoaded = true
                        )
                    }
                }
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    sessionExpired = result.unauthorized,
                    loadError = result.message
                )
            }
        }
    }

    fun setBandName(value: String) { _uiState.value = _uiState.value.copy(bandName = value.take(100), errorMessage = null) }
    fun setRole(value: String) { _uiState.value = _uiState.value.copy(role = value.take(100)) }
    fun setStartYear(value: String) { _uiState.value = _uiState.value.copy(startYear = yearDigits(value), errorMessage = null) }
    fun setEndYear(value: String) { _uiState.value = _uiState.value.copy(endYear = yearDigits(value), errorMessage = null) }
    fun setCurrent(value: Boolean) {
        _uiState.value = _uiState.value.copy(isCurrent = value, endYear = if (value) "" else _uiState.value.endYear)
    }

    private fun yearDigits(value: String) = value.filter(Char::isDigit).take(4)

    fun save() {
        val state = _uiState.value
        if (state.isSaving || !state.isLoaded) return
        val name = state.bandName.trim()
        val start = state.startYear.toIntOrNull()
        val end = if (state.isCurrent) null else state.endYear.toIntOrNull()
        val maxYear = Calendar.getInstance().get(Calendar.YEAR) + 1
        val error = when {
            name.isEmpty() -> "Enter the band's name."
            listOfNotNull(start, end).any { it !in 1900..maxYear } -> "Years must be between 1900 and $maxYear."
            start != null && end != null && end < start -> "The end year must be after the start year."
            else -> null
        }
        if (error != null) {
            _uiState.value = state.copy(errorMessage = error)
            return
        }
        val update = BandUpdate(
            bandName = name,
            role = state.role.trim().ifEmpty { null },
            startYear = start,
            endYear = end,
            isCurrent = state.isCurrent
        )
        run {
            val existing = state.existing
            if (existing == null) repository.addBand(update) else repository.updateBand(existing.id, update)
        }
    }

    fun remove() {
        val existing = _uiState.value.existing ?: return
        run { repository.removeBand(existing.id) }
    }

    private fun run(call: suspend () -> AuthResult<MusicianResponse>) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            _uiState.value = when (val result = call()) {
                is AuthResult.Success -> _uiState.value.copy(isSaving = false, done = true)
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
fun ProfileBandScreen(
    viewModel: ProfileBandViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmRemove by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }
    LaunchedEffect(uiState.done) {
        if (uiState.done) onDone()
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "Remove ${uiState.existing?.bandName ?: "this band"}?",
            text = "It will no longer show on your Musician Profile.",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            onConfirm = {
                confirmRemove = false
                viewModel.remove()
            },
            onDismiss = { confirmRemove = false }
        )
    }

    ProfileFormFrame(
        title = if (uiState.isNew) "Add band" else "Edit band",
        testTag = "band-edit-screen",
        onBack = if (uiState.isSaving) null else onBack
    ) {
        if (!uiState.isLoaded) {
            if (uiState.isLoading) LoadingFill()
            uiState.loadError?.let { ErrorText(it, Modifier.testTag("band-edit-error")) }
            return@ProfileFormFrame
        }
        val enabled = !uiState.isSaving

        Eyebrow("Musician profile")
        Text(
            if (uiState.isNew) "Add a band" else uiState.existing?.bandName.orEmpty(),
            style = MaterialTheme.typography.headlineMedium,
            color = GreywireText,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "A band you play or played in, for your profile. This doesn't add you to a band on Greywire.",
            style = MaterialTheme.typography.bodyMedium,
            color = GreywireMuted
        )
        Spacer(Modifier.height(24.dp))

        ProfileTextField(
            value = uiState.bandName,
            onValueChange = viewModel::setBandName,
            label = "Band name",
            enabled = enabled,
            testTag = "band-name-input"
        )
        Spacer(Modifier.height(16.dp))
        ProfileTextField(
            value = uiState.role,
            onValueChange = viewModel::setRole,
            label = "Role (optional)",
            enabled = enabled,
            testTag = "band-role-input",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileTextField(
                value = uiState.startYear,
                onValueChange = viewModel::setStartYear,
                label = "Start year",
                enabled = enabled,
                testTag = "band-start-year-input",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )
            ProfileTextField(
                value = uiState.endYear,
                onValueChange = viewModel::setEndYear,
                label = "End year",
                enabled = enabled && !uiState.isCurrent,
                testTag = "band-end-year-input",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("I'm in this band now", style = MaterialTheme.typography.titleMedium, color = GreywireText)
                Text("Shown as \"to Present\".", style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = uiState.isCurrent,
                onCheckedChange = viewModel::setCurrent,
                enabled = enabled,
                colors = greywireSwitchColors(),
                modifier = Modifier.testTag("band-current-switch")
            )
        }

        uiState.errorMessage?.let { FormError(it, "band-edit-error") }

        Spacer(Modifier.height(24.dp))
        SaveButton(
            isSaving = uiState.isSaving,
            onClick = viewModel::save,
            testTag = "band-save-button",
            label = if (uiState.isNew) "Add band" else "Save"
        )
        if (!uiState.isNew) {
            Spacer(Modifier.height(12.dp))
            GreywireGhostButton(
                onClick = { confirmRemove = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("remove-band-button")
            ) {
                Text("Remove band", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
