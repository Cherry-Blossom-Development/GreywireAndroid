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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.GreywireRepository
import com.cherryblossomdev.greywire.network.GreywirePedal
import com.cherryblossomdev.greywire.network.MembershipResponse
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireButton
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireTopBar
import com.cherryblossomdev.greywire.ui.components.StatusPill
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSignal
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Claim a pedal by its serial number (POST /api/greywire/claim), which makes
// the user a Greywire member. The web version is Breakroom's GreywireClaimPage.vue.

data class ClaimPedalUiState(
    val serial: String = "",
    /** The user's membership before this claim, to list pedals already held. */
    val membership: MembershipResponse? = null,
    val isClaiming: Boolean = false,
    val errorMessage: String? = null,
    /** The pedal just claimed (or found to be already yours); shows the success view. */
    val claimed: GreywirePedal? = null,
    val alreadyYours: Boolean = false,
    val sessionExpired: Boolean = false
)

class ClaimPedalViewModel(
    private val greywireRepository: GreywireRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClaimPedalUiState())
    val uiState: StateFlow<ClaimPedalUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            when (val result = greywireRepository.getMembership()) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(membership = result.data)
                // Not fatal: the form works without the list of pedals already held
                is AuthResult.Error -> if (result.unauthorized) {
                    _uiState.value = _uiState.value.copy(sessionExpired = true)
                }
            }
        }
    }

    fun updateSerial(value: String) {
        _uiState.value = _uiState.value.copy(serial = value, errorMessage = null)
    }

    fun claim() {
        val state = _uiState.value
        if (state.isClaiming || state.serial.isBlank()) return
        val serial = normalizeSerial(state.serial)
        val heldBefore = state.membership?.pedals.orEmpty().any { it.serial == serial }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClaiming = true, errorMessage = null)
            when (val result = greywireRepository.claimPedal(state.serial)) {
                is AuthResult.Success -> {
                    val membership = result.data.membership
                    val pedal = membership?.pedals?.firstOrNull { it.serial == serial }
                        ?: GreywirePedal(id = 0, model = "Greywire pedal", serial = serial)
                    _uiState.value = _uiState.value.copy(
                        isClaiming = false,
                        membership = membership ?: _uiState.value.membership,
                        claimed = pedal,
                        alreadyYours = heldBefore
                    )
                }
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(
                    isClaiming = false,
                    errorMessage = result.message,
                    sessionExpired = result.unauthorized
                )
            }
        }
    }

    companion object {
        /** Same rules as the backend's normalizeSerial (utilities/greywire.js), used only to find the claimed pedal. */
        fun normalizeSerial(input: String): String =
            input.trim().replace(Regex("^0x", RegexOption.IGNORE_CASE), "").replace(Regex("[\\s:-]"), "").uppercase()
    }
}

@Composable
fun ClaimPedalScreen(
    viewModel: ClaimPedalViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        GreywireTopBar(title = "Claim a pedal", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .testTag("claim-screen")
        ) {
            Spacer(Modifier.height(32.dp))
            val claimed = uiState.claimed
            if (claimed != null) {
                ClaimedView(claimed, uiState.alreadyYours, onDone)
            } else {
                ClaimForm(
                    uiState = uiState,
                    onSerialChange = viewModel::updateSerial,
                    onClaim = {
                        focusManager.clearFocus()
                        viewModel.claim()
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ClaimForm(
    uiState: ClaimPedalUiState,
    onSerialChange: (String) -> Unit,
    onClaim: () -> Unit
) {
    val isMember = uiState.membership?.member == true
    val heldPedals = uiState.membership?.pedals.orEmpty()

    Eyebrow("Membership")
    Text(
        text = if (isMember) "Claim another pedal" else "Claim your pedal",
        style = MaterialTheme.typography.headlineMedium,
        color = GreywireText,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Every Greywire pedal comes with a free membership, with no subscription, for as long as you own it. Enter the pedal's serial number to add it to your account.",
        style = MaterialTheme.typography.bodyLarge,
        color = GreywireMuted
    )

    if (heldPedals.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        GreywireCard {
            Text("Your pedals", style = MaterialTheme.typography.titleMedium, color = GreywireText)
            Spacer(Modifier.height(8.dp))
            heldPedals.forEach { pedal -> PedalRow(pedal) }
        }
    }

    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
        value = uiState.serial,
        onValueChange = onSerialChange,
        label = { Text("Pedal serial number") },
        placeholder = { Text("e.g. E4629C1B23456789", style = MonoSmallStyle) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoSmallStyle.fontFamily),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
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
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(onDone = { onClaim() }),
        enabled = !uiState.isClaiming,
        modifier = Modifier.fillMaxWidth().testTag("serial-input")
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "16 characters: the digits 0 to 9 and the letters A to F. Spaces and dashes are fine.",
        style = MaterialTheme.typography.bodySmall,
        color = GreywireMuted
    )

    uiState.errorMessage?.let { error ->
        Spacer(Modifier.height(16.dp))
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .testTag("claim-error")
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
    }

    Spacer(Modifier.height(24.dp))
    GreywireButton(
        onClick = onClaim,
        enabled = !uiState.isClaiming && uiState.serial.isNotBlank(),
        modifier = Modifier.fillMaxWidth().testTag("claim-button")
    ) {
        if (uiState.isClaiming) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Claim my pedal")
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(
        text = "Bought a pedal second-hand and it's already claimed? Contact support and we'll move it to your account.",
        style = MaterialTheme.typography.bodySmall,
        color = GreywireMuted
    )
}

@Composable
private fun ClaimedView(pedal: GreywirePedal, alreadyYours: Boolean, onDone: () -> Unit) {
    Eyebrow("Membership")
    Text(
        text = if (alreadyYours) "Already yours" else "You're in!",
        style = MaterialTheme.typography.headlineMedium,
        color = GreywireText,
        modifier = Modifier.semantics { heading() }.testTag("claim-success")
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = if (alreadyYours) {
            "This pedal is already registered to your account."
        } else {
            "Your ${pedal.model} is now registered to you, and membership is free for as long as you own it."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = GreywireMuted
    )
    Spacer(Modifier.height(20.dp))
    GreywireCard {
        StatusPill("Member", dotColor = GreywireSignal)
        Spacer(Modifier.height(12.dp))
        PedalRow(pedal)
    }
    Spacer(Modifier.height(24.dp))
    GreywireButton(onClick = onDone, modifier = Modifier.fillMaxWidth().testTag("claim-done-button")) {
        Text("Done")
    }
}

@Composable
private fun PedalRow(pedal: GreywirePedal) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pedal.model, style = MaterialTheme.typography.bodyLarge, color = GreywireText)
        Text(pedal.serial, style = MonoSmallStyle, color = GreywireMuted)
    }
}
