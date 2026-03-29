package com.example.nexus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nexus.ui.auth.AuthViewModel
import com.example.nexus.ui.auth.LoginScreen
import com.example.nexus.ui.auth.RegisterScreen
import com.example.nexus.ui.compose.ComposePostScreen
import com.example.nexus.ui.feed.feedNavGraph
import com.example.nexus.ui.screen.chat.ChatDetailScreen
import com.example.nexus.ui.screen.chat.ChatListScreen
import com.example.nexus.ui.screen.map.MapScreen
import com.example.nexus.ui.screen.profile.ProfileScreen
import com.example.nexus.ui.theme.NexusTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * MainActivity
 * Single-activity architecture for the entire Nexus application
 * Handles navigation between auth flow and main app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusTheme {
                AppContent()
            }
        }
    }
}

/**
 * AppContent
 * Determines whether to show auth screens or main app based on login state
 * Automatically navigates user if already authenticated
 * Responds to auth state changes (e.g., logout) in real-time
 */
@Composable
fun AppContent(viewModel: AuthViewModel = hiltViewModel()) {
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (isUserLoggedIn) "main" else "auth"
    ) {
        composable("auth") {
            AuthNavigation(
                onNavigateToMain = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainApp(
                onLogout = {
                    viewModel.logout()
                    navController.navigate("auth") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * AuthNavigation
 * Nested navigation for authentication screens (login, register)
 */
@Composable
fun AuthNavigation(onNavigateToMain: () -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onNavigateToMain = onNavigateToMain
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToMain = onNavigateToMain
            )
        }
    }
}

/**
 * Main app with bottom navigation
 */
@Composable
fun MainApp(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                BOTTOM_NAV_ITEMS.forEach { navItem ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = navItem.icon, contentDescription = navItem.label) },
                        label = { Text(navItem.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == navItem.route } == true,
                        onClick = {
                            navController.navigate(navItem.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Track swipe gesture
        var dragAmount = 0f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val currentRoute = navController
                                .currentBackStackEntry?.destination?.route
                            // Only swipe on main tabs, not sub-screens
                            val mainRoutes = BOTTOM_NAV_ITEMS.map { it.route }
                            if (currentRoute in mainRoutes) {
                                if (dragAmount < -100f) {
                                    // Swiped left → next tab
                                    navigateToAdjacentTab(currentRoute, navController, "left")
                                } else if (dragAmount > 100f) {
                                    // Swiped right → previous tab
                                    navigateToAdjacentTab(currentRoute, navController, "right")
                                }
                            }
                            dragAmount = 0f
                        },
                        onHorizontalDrag = { _, dragDelta ->
                            dragAmount += dragDelta
                        }
                    )
                }
        ) {
            NavHost(
                navController = navController,
                startDestination = "feed",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                feedNavGraph(navController)
                composable("compose_post") {
                    ComposePostScreen(
                        onBackClick = { navController.popBackStack() },
                        onPostPublished = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("map") {
                    MapScreen(
                        onNavigateToComposer = { latitude, longitude ->
                            navController.navigate("compose_post")
                        }
                    )
                }
                composable("chat") {
                    ChatListScreen(
                        onNavigateToNewChat = {
                            navController.navigate("new_chat")
                        },
                        onNavigateToChatDetail = { chatId ->
                            navController.navigate("chat_detail/$chatId")
                        }
                    )
                }
                composable("chat_detail/{chatId}") { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    ChatDetailScreen(
                        chatId = chatId,
                        chatName = "",
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
                composable("new_chat") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("New Chat - Coming Soon")
                    }
                }
                composable("profile") {
                    ProfileScreen(
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

private fun navigateToAdjacentTab(
    currentRoute: String?,
    navController: androidx.navigation.NavController,
    direction: String // "left" or "right"
) {
    val routes = BOTTOM_NAV_ITEMS.map { it.route }
    val currentIndex = routes.indexOf(currentRoute)
    if (currentIndex == -1) return

    val targetIndex = if (direction == "right") {
        currentIndex - 1  // swipe right = go to previous tab
    } else {
        currentIndex + 1  // swipe left = go to next tab
    }

    if (targetIndex < 0 || targetIndex >= routes.size) return

    navController.navigate(routes[targetIndex]) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Bottom navigation items configuration
 */
private val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(
        label = "Feed",
        icon = Icons.Default.Home,
        route = "feed"
    ),
    BottomNavItem(
        label = "Map",
        icon = Icons.Default.Map,
        route = "map"
    ),
    BottomNavItem(
        label = "Chat",
        icon = Icons.Default.Chat,
        route = "chat"
    ),
    BottomNavItem(
        label = "Profile",
        icon = Icons.Default.Person,
        route = "profile"
    )
)

data class BottomNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

