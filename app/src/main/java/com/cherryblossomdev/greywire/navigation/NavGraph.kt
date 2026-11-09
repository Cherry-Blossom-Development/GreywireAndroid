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
import com.cherryblossomdev.greywire.ui.screens.HomeScreen
import com.cherryblossomdev.greywire.ui.screens.HomeViewModel
import com.cherryblossomdev.greywire.ui.screens.LoginScreen
import com.cherryblossomdev.greywire.ui.screens.LoginViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
}

@Composable
fun GreywireNavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deps = remember { AppContainer(context.applicationContext) }

    // Current user ID (set after login); later screens will need it
    val currentUserId = remember { mutableIntStateOf(0) }

    val startDestination = if (deps.authRepository.isLoggedIn()) Screen.Home.route else Screen.Login.route

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
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val viewModel = remember { HomeViewModel(deps.authRepository, deps.greywireRepository) }
            HomeScreen(
                viewModel = viewModel,
                onLogout = ::performLogout,
                onSessionExpired = ::performLogout
            )
        }
    }
}
