package com.example.nexus.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * AuthNavGraph
 * Navigation graph for the authentication flow
 * Routes: splash -> login/register
 */
@Composable
fun AuthNavGraph(
    onNavigateToMain: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        route = "auth"
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToMain = onNavigateToMain,
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onNavigateToMain = {
                    onNavigateToMain()
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateToMain = {
                    onNavigateToMain()
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
    }
}
