package com.cherryblossomdev.greywire.ui.screens

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.MAX_UPLOAD_BYTES
import com.cherryblossomdev.greywire.data.SessionsRepository
import com.cherryblossomdev.greywire.data.tooLargeMessage
import com.cherryblossomdev.greywire.network.Band
import com.cherryblossomdev.greywire.network.Instrument
import com.cherryblossomdev.greywire.network.Session
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireButton
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireGhostButton
import com.cherryblossomdev.greywire.ui.components.GreywireTopBar
import com.cherryblossomdev.greywire.ui.components.greywireTextFieldColors
import com.cherryblossomdev.greywire.ui.components.popupTestTags
import com.cherryblossomdev.greywire.ui.components.readableWidth
import com.cherryblossomdev.greywire.ui.theme.GreywireBg
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

// Uploading a recording (roadmap 2.2): pick an audio file, name it, say
// whether it's a band practice or a solo take, and when it was recorded.
// Opens inside the Sessions tab, like a session. Uses the single-request
// upload (POST /api/sessions), as the Prosaurus app does; the server
// transcodes it before answering. Over the free-tier limit it answers 402,
// and the screen hands the message to the paywall.

/** An audio file picked for upload, copied into the app's cache. */
data class PickedAudio(val file: File, val fileName: String, val mimeType: String, val size: Long)

/** The earliest recording date the backend accepts (MIN_RECORDED_YEAR). */
private const val MIN_RECORDED_YEAR = 1950

enum class UploadType(val apiValue: String, val label: String) {
    Band("band", "Band practice"),
    Solo("individual", "Solo take")
}

data class SessionUploadUiState(
    val picked: PickedAudio? = null,
    val isPreparing: Boolean = false,
    val name: String = "",
    val type: UploadType = UploadType.Band,
    val bands: List<Band> = emptyList(),
    val bandId: Int? = null,
    val instruments: List<Instrument> = emptyList(),
    val instrumentId: Int? = null,
    /** UTC midnight of the recording date (what DatePicker uses), or null for no date. */
    val recordedAtMillis: Long? = todayUtcMillis(),
    val isUploading: Boolean = false,
    val errorMessage: String? = null,
    /** The backend's message when a free-tier limit was reached (402). */
    val paywallMessage: String? = null,
    val uploaded: Session? = null,
    val sessionExpired: Boolean = false
) {
    val canUpload: Boolean get() = picked != null && !isPreparing && !isUploading
}

class SessionUploadViewModel(private val repository: SessionsRepository, private val cacheDir: File) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUploadUiState())
    val uiState: StateFlow<SessionUploadUiState> = _uiState

    /** Loads the bands and instruments for the pickers; neither is required to upload. */
    fun load() {
        viewModelScope.launch {
            val bands = async { repository.getBands() }
            val instruments = async { repository.getInstruments() }
            val bandsResult = bands.await()
            if (bandsResult is AuthResult.Error && bandsResult.unauthorized) {
                _uiState.value = _uiState.value.copy(sessionExpired = true)
                return@launch
            }
            val activeBands = (bandsResult as? AuthResult.Success)?.data?.bands.orEmpty().filter { it.status != "invited" }
            val state = _uiState.value
            _uiState.value = state.copy(
                bands = activeBands,
                // One band: pick it. Otherwise the user chooses (or leaves it empty).
                bandId = state.bandId ?: activeBands.singleOrNull()?.id,
                instruments = (instruments.await() as? AuthResult.Success)?.data?.instruments.orEmpty()
            )
        }
    }

    /** Copies the picked file into the cache, so it can be sent as a multipart part. */
    fun pick(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPreparing = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) { copyToCache(resolver, uri) }
            result.fold(
                onSuccess = { picked ->
                    discard()
                    val state = _uiState.value
                    _uiState.value = state.copy(
                        picked = picked,
                        isPreparing = false,
                        // Name it after the file unless the user already typed one
                        name = state.name.ifBlank { picked.fileName.substringBeforeLast('.') }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isPreparing = false, errorMessage = e.message ?: "Couldn't open that file")
                }
            )
        }
    }

    fun setName(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun setType(type: UploadType) { _uiState.value = _uiState.value.copy(type = type) }
    fun setBand(bandId: Int?) { _uiState.value = _uiState.value.copy(bandId = bandId) }
    fun setInstrument(instrumentId: Int?) { _uiState.value = _uiState.value.copy(instrumentId = instrumentId) }
    fun setRecordedAt(millis: Long?) { _uiState.value = _uiState.value.copy(recordedAtMillis = millis) }
    fun paywallShown() { _uiState.value = _uiState.value.copy(paywallMessage = null) }

    fun upload() {
        val state = _uiState.value
        val picked = state.picked ?: return
        if (!state.canUpload) return
        viewModelScope.launch {
            _uiState.value = state.copy(isUploading = true, errorMessage = null)
            val result = repository.upload(
                file = picked.file,
                fileName = picked.fileName,
                mimeType = picked.mimeType,
                name = state.name.trim(),
                sessionType = state.type.apiValue,
                recordedAt = state.recordedAtMillis?.let(::formatApiDate),
                bandId = if (state.type == UploadType.Band) state.bandId else null,
                instrumentId = if (state.type == UploadType.Solo) state.instrumentId else null
            )
            _uiState.value = when (result) {
                is AuthResult.Success -> {
                    discard()
                    _uiState.value.copy(isUploading = false, picked = null, uploaded = result.data.session)
                }
                is AuthResult.Error -> _uiState.value.copy(
                    isUploading = false,
                    sessionExpired = result.unauthorized,
                    paywallMessage = if (result.requiresSubscription) result.message else null,
                    errorMessage = if (result.requiresSubscription) null else result.message
                )
            }
        }
    }

    override fun onCleared() {
        discard()
    }

    /** Deletes the cached copy of the picked file. The form calls it when it closes. */
    fun discard() {
        _uiState.value.picked?.file?.delete()
    }

    private fun copyToCache(resolver: ContentResolver, uri: Uri): Result<PickedAudio> = runCatching {
        var fileName = "recording"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { fileName = cursor.getString(it) }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { size = cursor.getLong(it) }
            }
        }
        // Check the size first, so a huge file isn't copied for nothing
        if (size > MAX_UPLOAD_BYTES) throw IllegalArgumentException(tooLargeMessage(size))

        val dir = File(cacheDir, "uploads").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.${fileName.substringAfterLast('.', "audio")}")
        val input = resolver.openInputStream(uri) ?: throw IllegalStateException("Couldn't open that file")
        input.use { src -> file.outputStream().use { src.copyTo(it) } }
        if (file.length() > MAX_UPLOAD_BYTES) {
            val length = file.length()
            file.delete()
            throw IllegalArgumentException(tooLargeMessage(length))
        }
        PickedAudio(file, fileName, resolver.getType(uri) ?: "application/octet-stream", file.length())
    }
}

/** Today as UTC midnight of the local date, the way DatePicker counts. */
private fun todayUtcMillis(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcFormat(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

/** "2026-11-24", what the backend takes for recorded_at. */
private fun formatApiDate(millis: Long): String = utcFormat("yyyy-MM-dd").format(millis)

/** "Nov 24, 2026", like the rest of the app. */
private fun formatPickedDate(millis: Long): String = utcFormat("MMM d, yyyy").format(millis)

/** "4.2 MB". */
private fun formatFileSize(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))

@Composable
fun SessionUploadScreen(
    viewModel: SessionUploadViewModel,
    onBack: () -> Unit,
    onUploaded: (Session?) -> Unit,
    onShowPaywall: (String) -> Unit,
    onSessionExpired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.pick(context.contentResolver, it) }
    }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }
    LaunchedEffect(uiState.uploaded) {
        uiState.uploaded?.let(onUploaded)
    }
    LaunchedEffect(uiState.paywallMessage) {
        uiState.paywallMessage?.let {
            viewModel.paywallShown()
            onShowPaywall(it)
        }
    }

    if (showDatePicker) {
        RecordedDateDialog(
            initialMillis = uiState.recordedAtMillis,
            onConfirm = {
                viewModel.setRecordedAt(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding().testTag("upload-screen")) {
        GreywireTopBar(title = "Upload", onBack = if (uiState.isUploading) null else onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .readableWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Eyebrow("Sessions")
            Text(
                "Upload a recording",
                style = MaterialTheme.typography.headlineMedium,
                color = GreywireText,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "MP3, WAV, M4A, FLAC, OGG and other audio files, up to 100 MB.",
                style = MaterialTheme.typography.bodyMedium,
                color = GreywireMuted
            )
            Spacer(Modifier.height(24.dp))

            FileCard(uiState, enabled = !uiState.isUploading, onPick = { picker.launch("audio/*") })
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::setName,
                label = { Text("Name") },
                placeholder = { Text("Untitled Session") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = greywireTextFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                enabled = !uiState.isUploading,
                modifier = Modifier.fillMaxWidth().testTag("session-name-input")
            )
            Spacer(Modifier.height(20.dp))

            FieldLabel("Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                UploadType.entries.forEach { type ->
                    ChoiceButton(
                        label = type.label,
                        selected = uiState.type == type,
                        enabled = !uiState.isUploading,
                        onClick = { viewModel.setType(type) },
                        modifier = Modifier.weight(1f).testTag("upload-type-${type.apiValue}")
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            when (uiState.type) {
                UploadType.Band -> if (uiState.bands.isNotEmpty()) {
                    PickerField(
                        label = "Band",
                        noneLabel = "No band",
                        options = uiState.bands.map { it.id to it.name },
                        selectedId = uiState.bandId,
                        enabled = !uiState.isUploading,
                        onSelect = viewModel::setBand,
                        testTag = "band-picker"
                    )
                    Spacer(Modifier.height(20.dp))
                }
                UploadType.Solo -> if (uiState.instruments.isNotEmpty()) {
                    PickerField(
                        label = "Instrument (optional)",
                        noneLabel = "No instrument",
                        options = uiState.instruments.map { it.id to it.name },
                        selectedId = uiState.instrumentId,
                        enabled = !uiState.isUploading,
                        onSelect = viewModel::setInstrument,
                        testTag = "instrument-picker"
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            FieldLabel("Recorded")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    uiState.recordedAtMillis?.let(::formatPickedDate) ?: "No date",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GreywireText,
                    modifier = Modifier.weight(1f).testTag("recorded-date")
                )
                GreywireGhostButton(
                    onClick = { showDatePicker = true },
                    enabled = !uiState.isUploading,
                    modifier = Modifier.testTag("recorded-date-button")
                ) {
                    Text("Change", style = MaterialTheme.typography.labelMedium)
                }
            }

            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("upload-error").semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            Spacer(Modifier.height(24.dp))
            GreywireButton(
                onClick = viewModel::upload,
                enabled = uiState.canUpload,
                modifier = Modifier.fillMaxWidth().testTag("upload-submit-button")
            ) {
                if (uiState.isUploading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Uploading...")
                } else {
                    Text("Upload")
                }
            }
            if (uiState.isUploading) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "The server prepares the recording for playback before it's saved, which can take a minute for long files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GreywireMuted
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FileCard(uiState: SessionUploadUiState, enabled: Boolean, onPick: () -> Unit) {
    val picked = uiState.picked
    GreywireCard(modifier = Modifier.testTag("upload-file-card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AudioFile, contentDescription = null, tint = if (picked != null) GreywireCopper else GreywireMuted)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isPreparing -> Text("Opening the file...", style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
                    picked != null -> {
                        Text(picked.fileName, style = MaterialTheme.typography.titleMedium, color = GreywireText, maxLines = 2)
                        Text(formatFileSize(picked.size), style = MonoSmallStyle, color = GreywireMuted)
                    }
                    else -> Text("No file chosen", style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GreywireGhostButton(
            onClick = onPick,
            enabled = enabled && !uiState.isPreparing,
            modifier = Modifier.fillMaxWidth().testTag("choose-file-button")
        ) {
            Text(if (picked == null) "Choose audio file" else "Choose a different file", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = GreywireMuted, modifier = Modifier.padding(bottom = 8.dp))
}

/** One of a row of mutually exclusive options: copper when selected. */
@Composable
internal fun ChoiceButton(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GreywireCopper else GreywireSurface)
            .border(1.dp, if (selected) GreywireCopper else GreywireLine, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                this.selected = selected
                role = Role.RadioButton
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) GreywireBg else GreywireText)
    }
}

/** A dropdown of [options] (id to name) plus a "none" choice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PickerField(
    label: String,
    noneLabel: String,
    options: List<Pair<Int, String>>,
    selectedId: Int?,
    enabled: Boolean,
    onSelect: (Int?) -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selectedId }?.second ?: noneLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(10.dp),
            colors = greywireTextFieldColors(),
            enabled = enabled,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth()
                .testTag(testTag)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = GreywireSurface,
            modifier = Modifier.popupTestTags()
        ) {
            DropdownMenuItem(
                text = { Text(noneLabel, color = GreywireMuted) },
                onClick = { onSelect(null); expanded = false }
            )
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, color = GreywireText) },
                    onClick = { onSelect(id); expanded = false }
                )
            }
        }
    }
}

/** A date picker limited to the dates the backend accepts: 1950 to today. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordedDateDialog(initialMillis: Long?, onConfirm: (Long?) -> Unit, onDismiss: () -> Unit) {
    val today = remember { todayUtcMillis() }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = MIN_RECORDED_YEAR..currentYear,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            override fun isSelectableYear(year: Int) = year in MIN_RECORDED_YEAR..currentYear
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.popupTestTags(),
        confirmButton = {
            TextButton(onClick = { onConfirm(state.selectedDateMillis) }, modifier = Modifier.testTag("date-confirm-button")) {
                Text("OK", color = GreywireCopper)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GreywireText) }
        }
    ) {
        DatePicker(state = state)
    }
}
