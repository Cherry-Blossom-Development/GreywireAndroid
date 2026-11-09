package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.cherryblossomdev.greywire.data.AuthRepository
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.GreywireRepository
import com.cherryblossomdev.greywire.network.MembershipResponse
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireGhostButton
import com.cherryblossomdev.greywire.ui.components.GreywireHeader
import com.cherryblossomdev.greywire.ui.components.SectionHeading
import com.cherryblossomdev.greywire.ui.components.StatusPill
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSignal
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val username: String? = null,
    val membership: MembershipResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** The saved token was rejected (expired or revoked): send the user to sign in. */
    val sessionExpired: Boolean = false
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val greywireRepository: GreywireRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(username = authRepository.getStoredUsername()))
    val uiState: StateFlow<HomeUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val me = authRepository.getMe()
            if (me is AuthResult.Error && me.unauthorized) {
                _uiState.value = _uiState.value.copy(isLoading = false, sessionExpired = true)
                return@launch
            }
            val username = (me as? AuthResult.Success)?.data?.username ?: _uiState.value.username

            when (val result = greywireRepository.getMembership()) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    username = username, membership = result.data, isLoading = false
                )
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(
                    username = username,
                    isLoading = false,
                    sessionExpired = result.unauthorized,
                    errorMessage = result.message
                )
            }
        }
    }
}

// The pedals shown on the home screen, worded as on the website's home page.
// Static for now; this should come from the backend once there's a pedal catalog.
private data class PedalInfo(val name: String, val type: String, val blurb: String, val status: String)

private val LineworkPedals = listOf(
    PedalInfo(
        name = "Flashover",
        type = "Analog distortion / overdrive",
        blurb = "A classic op-amp distortion with a tone control, soft or hard clipping, and knobs Switchyard can set for you.",
        status = "Prototyping"
    ),
    PedalInfo(
        name = "Switchyard",
        type = "Control pedal",
        blurb = "Two footswitches, a song and part readout, and a direct connection to your phone. The brain of the board.",
        status = "Design"
    )
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLogout: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        GreywireHeader(action = {
            GreywireGhostButton(onClick = onLogout, modifier = Modifier.testTag("logout-button")) {
                Text("Sign out", style = MaterialTheme.typography.labelMedium)
            }
        })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Hero (`.hero`)
            Spacer(Modifier.height(40.dp))
            Eyebrow(uiState.username?.let { "Signed in as $it" } ?: "Linework series")
            Text(
                text = "Your pedalboard,\nconnected.",
                style = MaterialTheme.typography.displaySmall,
                color = GreywireText,
                modifier = Modifier.semantics { heading() }.testTag("home-title")
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Set up sounds and setlists here, and Switchyard changes every Linework pedal on your board with one tap.",
                style = MaterialTheme.typography.bodyLarge,
                color = GreywireMuted
            )
            Spacer(Modifier.height(40.dp))

            // Membership
            SectionDivider()
            Eyebrow("Membership")
            SectionHeading("Your Greywire account")
            Spacer(Modifier.height(16.dp))
            MembershipCard(uiState)
            Spacer(Modifier.height(40.dp))

            // Pedals (`#pedals`)
            SectionDivider()
            Eyebrow("The pedals")
            SectionHeading("The first two Linework pedals")
            Spacer(Modifier.height(16.dp))
            LineworkPedals.forEach { pedal ->
                PedalCard(pedal)
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = GreywireLine, thickness = 1.dp)
    Spacer(Modifier.height(32.dp))
}

@Composable
private fun MembershipCard(uiState: HomeUiState) {
    GreywireCard(modifier = Modifier.testTag("membership-card")) {
        val membership = uiState.membership
        when {
            membership == null && uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                    color = GreywireCopper,
                    strokeWidth = 2.dp
                )
            }
            membership == null -> {
                Text(
                    text = uiState.errorMessage ?: "Couldn't load your membership.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GreywireMuted
                )
            }
            membership.member -> {
                StatusPill("Member", dotColor = GreywireSignal)
                Spacer(Modifier.height(12.dp))
                if (membership.pedals.isEmpty()) {
                    Text(
                        text = "Your membership was granted by Greywire Studio. Pedals you claim will show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreywireMuted
                    )
                } else {
                    Text("Your pedals", style = MaterialTheme.typography.titleMedium, color = GreywireText)
                    Spacer(Modifier.height(8.dp))
                    membership.pedals.forEach { pedal ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pedal.model, style = MaterialTheme.typography.bodyLarge, color = GreywireText)
                            Text(pedal.serial, style = MonoSmallStyle, color = GreywireMuted)
                        }
                    }
                }
            }
            else -> {
                StatusPill("Not a member yet")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Greywire membership comes with a Greywire pedal. Once you own one, claim it with its serial number to join.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GreywireMuted
                )
            }
        }
    }
}

@Composable
private fun PedalCard(pedal: PedalInfo) {
    GreywireCard {
        Text(pedal.name, style = MaterialTheme.typography.titleLarge, color = GreywireText)
        Spacer(Modifier.height(2.dp))
        Text(pedal.type, style = MonoSmallStyle, color = GreywireMuted)
        Spacer(Modifier.height(10.dp))
        Text(pedal.blurb, style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
        Spacer(Modifier.height(14.dp))
        StatusPill(pedal.status)
    }
}
