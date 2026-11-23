package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.BillingRepository
import com.cherryblossomdev.greywire.network.BillingPlanResponse
import com.cherryblossomdev.greywire.ui.components.GreywireButton
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireSubScreen
import com.cherryblossomdev.greywire.ui.components.StatusPill
import com.cherryblossomdev.greywire.ui.components.formatShortDate
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSignal
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Shown when the backend answers 402 (a free-tier limit was reached:
// requiresSubscription). In-app purchase isn't built yet (roadmap 1.11), so
// this shows the plan status and points to the website to subscribe.

data class PaywallUiState(
    val plan: BillingPlanResponse? = null,
    val isLoading: Boolean = true
)

class PaywallViewModel(private val billingRepository: BillingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            // Not fatal: without the plan the screen just says "Free tier"
            val plan = (billingRepository.getPlan() as? AuthResult.Success)?.data
            _uiState.value = PaywallUiState(plan = plan, isLoading = false)
        }
    }
}

@Composable
fun PaywallScreen(viewModel: PaywallViewModel, message: String, onDismiss: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    GreywireSubScreen(title = "Subscription Required", onBack = onDismiss, testTag = "paywall-screen") {
        Spacer(Modifier.height(32.dp))
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = GreywireCopper,
            modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Feature Limit Reached",
            style = MaterialTheme.typography.headlineMedium,
            color = GreywireText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() }
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = GreywireMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("paywall-message")
        )
        Spacer(Modifier.height(32.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                color = GreywireCopper,
                strokeWidth = 2.dp
            )
        } else {
            PlanStatusCard(uiState.plan)
        }
        Spacer(Modifier.height(32.dp))

        GreywireCard {
            Text("Greywire Studio Pro", style = MaterialTheme.typography.titleLarge, color = GreywireText)
            Spacer(Modifier.height(12.dp))
            BenefitRow("Unlimited session recordings")
            BenefitRow("Unlimited band memberships")
            BenefitRow("Priority support")
            BenefitRow("Works on Greywire and Prosaurus")
        }
        Spacer(Modifier.height(24.dp))

        Text(
            "In-app purchase coming soon. For now, subscribe at greywirestudio.com to unlock all features.",
            style = MaterialTheme.typography.bodySmall,
            color = GreywireMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        GreywireButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("paywall-dismiss-button")) {
            Text("Got it")
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PlanStatusCard(plan: BillingPlanResponse?) {
    GreywireCard {
        Text("Your Plan", style = MaterialTheme.typography.titleLarge, color = GreywireText)
        Spacer(Modifier.height(12.dp))
        if (plan == null) {
            Text("Free tier", style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
            return@GreywireCard
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Status", style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
            StatusPill(
                text = if (plan.subscribed) "Pro" else "Free",
                dotColor = if (plan.subscribed) GreywireSignal else GreywireCopper
            )
        }
        if (plan.subscribed && plan.expiresAt != null) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Renews", style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
                Text(formatShortDate(plan.expiresAt), style = MonoSmallStyle, color = GreywireText)
            }
        }
        val membership = plan.greywire
        if (membership != null && membership.member) {
            Spacer(Modifier.height(12.dp))
            Text(
                "You're a Greywire member with ${membership.pedals.size} pedal(s), which gives you unlimited access on Greywire.",
                style = MaterialTheme.typography.bodySmall,
                color = GreywireSignal
            )
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreywireSignal, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
    }
}
