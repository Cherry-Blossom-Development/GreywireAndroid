package com.cherryblossomdev.greywire.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cherryblossomdev.greywire.AppContainer
import com.cherryblossomdev.greywire.ui.screens.EulaGateScreen
import com.cherryblossomdev.greywire.ui.screens.EulaGateViewModel
import com.cherryblossomdev.greywire.ui.screens.EulaScreen
import com.cherryblossomdev.greywire.ui.screens.HomeScreen
import com.cherryblossomdev.greywire.ui.screens.HomeViewModel
import com.cherryblossomdev.greywire.ui.screens.LegalScreen
import com.cherryblossomdev.greywire.ui.screens.LegalViewModel
import com.cherryblossomdev.greywire.ui.screens.LoginScreen
import com.cherryblossomdev.greywire.ui.screens.LoginViewModel
import com.cherryblossomdev.greywire.ui.screens.PrivacyPolicyScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object EulaGate : Screen("eula-gate")
    object Home : Screen("home")
    object Legal : Screen("legal")
    object Eula : Screen("eula")
    object PrivacyPolicy : Screen("privacy-policy")
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

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            val viewModel = remember { LoginViewModel(deps.authRepository) }
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { userId ->
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
                onLogout = ::performLogout,
                onSessionExpired = ::performLogout,
                onOpenLegal = { navController.navigate(Screen.Legal.route) }
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

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
    }
}
