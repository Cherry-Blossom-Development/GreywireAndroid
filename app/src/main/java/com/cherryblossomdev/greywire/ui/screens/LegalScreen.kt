package com.cherryblossomdev.greywire.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthRepository
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.ui.components.GreywireTopBar
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireCopperSoft
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSignal
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import com.cherryblossomdev.greywire.ui.theme.PlexMono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// The Legal Documents screen: the EULA (with the user's acceptance status) and
// the Privacy Policy. Matches GreywireiPhone's LegalView.

data class LegalUiState(
    val isLoading: Boolean = true,
    val eulaAccepted: Boolean = false,
    val eulaAcceptedAt: String? = null,
    val sessionExpired: Boolean = false
)

class LegalViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LegalUiState())
    val uiState: StateFlow<LegalUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = when (val result = authRepository.getEulaStatus()) {
                is AuthResult.Success -> LegalUiState(
                    isLoading = false,
                    eulaAccepted = result.data.accepted,
                    eulaAcceptedAt = result.data.acceptedAt
                )
                // As on iOS: if the status can't be loaded, don't show the
                // user a "Pending" EULA they can't act on.
                is AuthResult.Error -> LegalUiState(
                    isLoading = false,
                    eulaAccepted = true,
                    sessionExpired = result.unauthorized
                )
            }
        }
    }
}

private enum class LegalDocumentStatus { Accepted, Acknowledged, Pending }

@Composable
fun LegalScreen(
    viewModel: LegalViewModel,
    onBack: () -> Unit,
    onOpenEula: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        GreywireTopBar(title = "Legal Documents", onBack = onBack)

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = GreywireCopper,
                    strokeWidth = 2.dp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Documents associated with your Greywire account.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GreywireMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LegalDocumentCard(
                    title = "End User License Agreement",
                    description = "Governs your use of the Greywire community.",
                    status = if (uiState.eulaAccepted) LegalDocumentStatus.Accepted else LegalDocumentStatus.Pending,
                    statusDate = uiState.eulaAcceptedAt,
                    onClick = onOpenEula,
                    modifier = Modifier.testTag("eula-card")
                )

                LegalDocumentCard(
                    title = "Privacy Policy",
                    description = "Describes how we collect, use, and protect your data.",
                    status = LegalDocumentStatus.Acknowledged,
                    statusDate = null,
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier.testTag("privacy-card")
                )
            }
        }
    }
}

@Composable
private fun LegalDocumentCard(
    title: String,
    description: String,
    status: LegalDocumentStatus,
    statusDate: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GreywireSurface, shape)
            .border(1.dp, GreywireLine, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = GreywireCopper)
            Text(description, style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
            if (status == LegalDocumentStatus.Accepted && statusDate != null) {
                Text("Accepted on ${formatLegalDate(statusDate)}", style = MonoSmallStyle, color = GreywireMuted)
            } else if (status == LegalDocumentStatus.Acknowledged) {
                Text(
                    "Acknowledged by creating an account",
                    style = MonoSmallStyle.copy(fontStyle = FontStyle.Italic),
                    color = GreywireMuted
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        StatusBadge(status)
    }
}

@Composable
private fun StatusBadge(status: LegalDocumentStatus) {
    val badgeText = MonoSmallStyle.copy(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 11.sp)
    when (status) {
        LegalDocumentStatus.Accepted, LegalDocumentStatus.Acknowledged -> Row(
            modifier = Modifier
                .background(GreywireSignal.copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreywireSignal, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (status == LegalDocumentStatus.Accepted) "Accepted" else "Acknowledged",
                style = badgeText,
                color = GreywireSignal
            )
        }
        LegalDocumentStatus.Pending -> Text(
            "Pending",
            style = badgeText,
            color = GreywireCopperSoft,
            modifier = Modifier
                .background(GreywireCopper.copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/** "2026-11-10T18:22:05.000Z" -> "November 10, 2026". Falls back to the raw string. */
private fun formatLegalDate(dateStr: String): String = try {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = parser.parse(dateStr.take(19))
    if (date != null) SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date) else dateStr
} catch (e: Exception) {
    dateStr
}
