package com.cherryblossomdev.greywire.ui.screens

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.ProfileRepository
import com.cherryblossomdev.greywire.network.Instrument
import com.cherryblossomdev.greywire.network.InstrumentUpdate
import com.cherryblossomdev.greywire.network.MusicianResponse
import com.cherryblossomdev.greywire.network.ProfileInstrument
import com.cherryblossomdev.greywire.ui.components.ErrorText
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireGhostButton
import com.cherryblossomdev.greywire.ui.components.LoadingFill
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Add or edit an instrument on the Musician Profile (roadmap 2.10), as the
// web's MusicianProfile.vue does: the instrument (when adding), skill level,
// years playing and whether it's the main one. Editing also offers Remove.

/** The skill levels the backend accepts, with the web's labels. */
internal val SkillLevels = listOf(
    "beginner" to "Beginner",
    "intermediate" to "Intermediate",
    "advanced" to "Advanced",
    "professional" to "Professional"
)

data class ProfileInstrumentUiState(
    /** Null when adding. */
    val existing: ProfileInstrument? = null,
    /** Instruments that can be added (not already on the profile). */
    val choices: List<Instrument> = emptyList(),
    val instrumentId: Int? = null,
    val skillLevel: String? = null,
    val yearsPlaying: String = "",
    val isPrimary: Boolean = false,
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

/** [rowId] is the user_instruments row to edit, or null to add one. */
class ProfileInstrumentViewModel(
    private val repository: ProfileRepository,
    private val rowId: Int?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileInstrumentUiState())
    val uiState: StateFlow<ProfileInstrumentUiState> = _uiState

    fun load() {
        if (_uiState.value.isLoaded || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            val profile = repository.getProfile()
            if (profile is AuthResult.Error) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sessionExpired = profile.unauthorized,
                    loadError = profile.message
                )
                return@launch
            }
            val musician = (profile as AuthResult.Success).data.user.musician
            val current = musician?.instruments.orEmpty()
            if (rowId != null) {
                val existing = current.firstOrNull { it.id == rowId }
                _uiState.value = if (existing == null) {
                    _uiState.value.copy(isLoading = false, loadError = "This instrument is no longer on your profile.")
                } else {
                    _uiState.value.copy(
                        existing = existing,
                        instrumentId = existing.instrumentId,
                        skillLevel = existing.skillLevel,
                        yearsPlaying = existing.yearsPlaying?.toString().orEmpty(),
                        isPrimary = existing.isPrimary,
                        isLoading = false,
                        isLoaded = true
                    )
                }
                return@launch
            }
            _uiState.value = when (val list = repository.getInstruments()) {
                is AuthResult.Success -> {
                    val taken = current.map { it.instrumentId }.toSet()
                    _uiState.value.copy(
                        choices = list.data.instruments.filter { it.id !in taken },
                        // The first instrument added is the main one, as on the web
                        isPrimary = current.isEmpty(),
                        isLoading = false,
                        isLoaded = true
                    )
                }
                is AuthResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    sessionExpired = list.unauthorized,
                    loadError = list.message
                )
            }
        }
    }

    fun setInstrument(id: Int?) { _uiState.value = _uiState.value.copy(instrumentId = id, errorMessage = null) }
    fun setSkillLevel(level: String?) { _uiState.value = _uiState.value.copy(skillLevel = level) }
    fun setYearsPlaying(value: String) {
        _uiState.value = _uiState.value.copy(yearsPlaying = value.filter(Char::isDigit).take(3), errorMessage = null)
    }
    fun setPrimary(value: Boolean) { _uiState.value = _uiState.value.copy(isPrimary = value) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || !state.isLoaded) return
        if (state.isNew && state.instrumentId == null) {
            _uiState.value = state.copy(errorMessage = "Choose an instrument.")
            return
        }
        val years = state.yearsPlaying.takeIf { it.isNotEmpty() }?.toInt()
        if (years != null && years > 100) {
            _uiState.value = state.copy(errorMessage = "Years playing must be a whole number from 0 to 100.")
            return
        }
        val update = InstrumentUpdate(
            instrumentId = if (state.isNew) state.instrumentId else null,
            skillLevel = state.skillLevel,
            yearsPlaying = years,
            isPrimary = state.isPrimary
        )
        run {
            val existing = state.existing
            if (existing == null) repository.addInstrument(update) else repository.updateInstrument(existing.id, update)
        }
    }

    fun remove() {
        val existing = _uiState.value.existing ?: return
        run { repository.removeInstrument(existing.id) }
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
fun ProfileInstrumentScreen(
    viewModel: ProfileInstrumentViewModel,
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
            title = "Remove ${uiState.existing?.name ?: "this instrument"}?",
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
        title = if (uiState.isNew) "Add instrument" else "Edit instrument",
        testTag = "instrument-edit-screen",
        onBack = if (uiState.isSaving) null else onBack
    ) {
        if (!uiState.isLoaded) {
            if (uiState.isLoading) LoadingFill()
            uiState.loadError?.let { ErrorText(it, Modifier.testTag("instrument-edit-error")) }
            return@ProfileFormFrame
        }
        val enabled = !uiState.isSaving

        Eyebrow("Musician profile")
        Text(
            uiState.existing?.name ?: "Add an instrument",
            style = MaterialTheme.typography.headlineMedium,
            color = GreywireText,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(24.dp))

        if (uiState.isNew) {
            if (uiState.choices.isEmpty()) {
                Text(
                    "Every instrument on the list is already on your profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GreywireMuted
                )
                return@ProfileFormFrame
            }
            PickerField(
                label = "Instrument",
                noneLabel = "Choose an instrument",
                options = uiState.choices.map { it.id to it.name },
                selectedId = uiState.instrumentId,
                enabled = enabled,
                onSelect = viewModel::setInstrument,
                testTag = "instrument-choice-picker"
            )
            Spacer(Modifier.height(16.dp))
        }

        PickerField(
            label = "Skill level (optional)",
            noneLabel = "Not set",
            options = SkillLevels.mapIndexed { index, (_, label) -> index to label },
            selectedId = SkillLevels.indexOfFirst { it.first == uiState.skillLevel }.takeIf { it >= 0 },
            enabled = enabled,
            onSelect = { index -> viewModel.setSkillLevel(index?.let { SkillLevels[it].first }) },
            testTag = "skill-level-picker"
        )
        Spacer(Modifier.height(16.dp))

        ProfileTextField(
            value = uiState.yearsPlaying,
            onValueChange = viewModel::setYearsPlaying,
            label = "Years playing (optional)",
            enabled = enabled,
            testTag = "years-playing-input",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Main instrument", style = MaterialTheme.typography.titleMedium, color = GreywireText)
                Text("Shown first, marked Main.", style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = uiState.isPrimary,
                onCheckedChange = viewModel::setPrimary,
                enabled = enabled,
                colors = greywireSwitchColors(),
                modifier = Modifier.testTag("primary-instrument-switch")
            )
        }

        uiState.errorMessage?.let { FormError(it, "instrument-edit-error") }

        Spacer(Modifier.height(24.dp))
        SaveButton(
            isSaving = uiState.isSaving,
            onClick = viewModel::save,
            testTag = "instrument-save-button",
            label = if (uiState.isNew) "Add instrument" else "Save"
        )
        if (!uiState.isNew) {
            Spacer(Modifier.height(12.dp))
            GreywireGhostButton(
                onClick = { confirmRemove = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("remove-instrument-button")
            ) {
                Text("Remove instrument", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
