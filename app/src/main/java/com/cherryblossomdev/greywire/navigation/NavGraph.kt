package com.cherryblossomdev.greywire.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cherryblossomdev.greywire.AppContainer
import com.cherryblossomdev.greywire.ui.components.GreywireBottomBar
import com.cherryblossomdev.greywire.ui.components.GreywireTab
import com.cherryblossomdev.greywire.ui.screens.ChatTabScreen
import com.cherryblossomdev.greywire.ui.screens.FriendsTabScreen
import com.cherryblossomdev.greywire.ui.screens.ProfileTabScreen
import com.cherryblossomdev.greywire.ui.screens.SessionsTabScreen
import com.cherryblossomdev.greywire.ui.screens.ClaimPedalScreen
import com.cherryblossomdev.greywire.ui.screens.ClaimPedalViewModel
import com.cherryblossomdev.greywire.ui.screens.EulaGateScreen
import com.cherryblossomdev.greywire.ui.screens.EulaGateViewModel
import com.cherryblossomdev.greywire.ui.screens.EulaScreen
import com.cherryblossomdev.greywire.ui.screens.ForgotPasswordScreen
import com.cherryblossomdev.greywire.ui.screens.ForgotPasswordViewModel
import com.cherryblossomdev.greywire.ui.screens.HomeScreen
import com.cherryblossomdev.greywire.ui.screens.HomeViewModel
import com.cherryblossomdev.greywire.ui.screens.LegalScreen
import com.cherryblossomdev.greywire.ui.screens.LegalViewModel
import com.cherryblossomdev.greywire.ui.screens.LoginScreen
import com.cherryblossomdev.greywire.ui.screens.LoginViewModel
import com.cherryblossomdev.greywire.ui.screens.PaywallScreen
import com.cherryblossomdev.greywire.ui.screens.PaywallViewModel
import com.cherryblossomdev.greywire.ui.screens.PrivacyPolicyScreen
import com.cherryblossomdev.greywire.ui.screens.SignupScreen
import com.cherryblossomdev.greywire.ui.screens.SignupViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object ForgotPassword : Screen("forgot-password")
    object EulaGate : Screen("eula-gate")
    // The five tabs, with the bottom bar (see GreywireTab)
    object Home : Screen(GreywireTab.Home.route)
    object Sessions : Screen(GreywireTab.Sessions.route)
    object Chat : Screen(GreywireTab.Chat.route)
    object Friends : Screen(GreywireTab.Friends.route)
    object Profile : Screen(GreywireTab.Profile.route)
    object ClaimPedal : Screen("claim-pedal")
    object Legal : Screen("legal")
    object Eula : Screen("eula")
    object PrivacyPolicy : Screen("privacy-policy")
    // Shown on a 402 (free-tier limit reached); the backend's message is the argument
    object Paywall : Screen("paywall?message={message}") {
        fun route(message: String) = "paywall?message=${Uri.encode(message)}"
    }
}

@Composable
fun GreywireNavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deps = remember { AppContainer(context.applicationContext) }

    // Current user ID (set after login); later screens will need it
    val currentUserId = remember { mutableIntStateOf(0) }

    // Signed-in launches go through the EULA check before Home
    val startDestination = if (deps.authRepository.isLoggedIn()) Screen.EulaGate.route else Screen.Login.route

    fun goToLogin() {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun performLogout() {
        currentUserId.intValue = 0
        scope.launch { deps.authRepository.logout() }
        goToLogin()
    }

    // For a result with requiresSubscription (a 402): explain the limit and the plan.
    // Nothing returns 402 yet in the features the app has (the limits are on
    // uploads and bands), matching iOS, where the paywall is also not yet triggered.
    @Suppress("unused")
    fun showPaywall(message: String) {
        navController.navigate(Screen.Paywall.route(message))
    }

    // Switching tabs keeps one copy of each tab, remembers its state, and
    // keeps Home at the bottom of the back stack (Back from a tab goes Home).
    // Home is always there: the EULA step replaces itself with it.
    fun selectTab(tab: GreywireTab) {
        navController.navigate(tab.route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // The bottom bar shows on the tabs only; Login, the EULA step and
    // sub-screens (Claim, Legal...) are full screen.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = GreywireTab.fromRoute(backStackEntry?.destination?.route)

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) GreywireBottomBar(selected = currentTab, onSelect = ::selectTab)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Login.route) {
                val viewModel = remember { LoginViewModel(deps.authRepository) }
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { userId ->
                        currentUserId.intValue = userId
                        navController.navigate(Screen.EulaGate.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onCreateAccount = { navController.navigate(Screen.Signup.route) },
                    onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
                )
            }

            composable(Screen.ForgotPassword.route) {
                val viewModel = remember { ForgotPasswordViewModel(deps.authRepository) }
                ForgotPasswordScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Signup.route) {
                val viewModel = remember { SignupViewModel(deps.authRepository) }
                SignupScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    // Signed in: on to the EULA step, with Login and Signup gone from the back stack
                    onSignupSuccess = { userId ->
                        currentUserId.intValue = userId
                        navController.navigate(Screen.EulaGate.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.EulaGate.route) {
                val viewModel = remember { EulaGateViewModel(deps.authRepository) }
                EulaGateScreen(
                    viewModel = viewModel,
                    onAccepted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.EulaGate.route) { inclusive = true }
                        }
                    },
                    onDecline = ::performLogout,
                    onSessionExpired = ::performLogout,
                    onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
                )
            }

            composable(Screen.Home.route) {
                val viewModel = remember { HomeViewModel(deps.authRepository, deps.greywireRepository) }
                HomeScreen(
                    viewModel = viewModel,
                    onSessionExpired = ::performLogout,
                    onOpenLegal = { navController.navigate(Screen.Legal.route) },
                    onClaimPedal = { navController.navigate(Screen.ClaimPedal.route) }
                )
            }

            composable(Screen.Sessions.route) { SessionsTabScreen() }
            composable(Screen.Chat.route) { ChatTabScreen() }
            composable(Screen.Friends.route) { FriendsTabScreen() }
            composable(Screen.Profile.route) {
                ProfileTabScreen(
                    username = remember { deps.authRepository.getStoredUsername() },
                    onLogout = ::performLogout
                )
            }

            composable(Screen.ClaimPedal.route) {
                val viewModel = remember { ClaimPedalViewModel(deps.greywireRepository, deps.authRepository) }
                ClaimPedalScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    // Home reloads the membership when it comes back on screen
                    onDone = { navController.popBackStack() },
                    onSessionExpired = ::performLogout
                )
            }

            composable(Screen.Legal.route) {
                val viewModel = remember { LegalViewModel(deps.authRepository) }
                LegalScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenEula = { navController.navigate(Screen.Eula.route) },
                    onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                    onSessionExpired = ::performLogout
                )
            }

            composable(Screen.Eula.route) {
                EulaScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
                )
            }

            composable(
                Screen.Paywall.route,
                arguments = listOf(navArgument("message") {
                    type = NavType.StringType
                    defaultValue = "You've reached the free plan's limit."
                })
            ) { entry ->
                val viewModel = remember { PaywallViewModel(deps.billingRepository) }
                PaywallScreen(
                    viewModel = viewModel,
                    message = entry.arguments?.getString("message").orEmpty(),
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
