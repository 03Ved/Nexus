package com.example.nexus.ui.auth

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

/**
 * SplashScreen
 * Animated entry point that checks Firebase authentication state
 * Routes to LoginScreen if not authenticated, or MainNavGraph if authenticated
 */
@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    Log.d("NEXUS_DEBUG", "SplashScreen is rendering")
    
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val authState by viewModel.authState.collectAsState()

    var isAnimationComplete by remember { mutableStateOf(false) }

    // Animate logo scale and alpha
    val scale by animateFloatAsState(
        targetValue = if (isAnimationComplete) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200
        ),
        label = "Logo scale animation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isAnimationComplete) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200
        ),
        label = "Logo alpha animation"
    )

    // Trigger animation and navigation
    LaunchedEffect(Unit) {
        isAnimationComplete = true
        delay(1500)  // Show splash for 1.5 seconds

        // Navigate based on authentication state
        Log.d("NEXUS_DEBUG", "SplashScreen LaunchedEffect: isUserLoggedIn=$isUserLoggedIn, authState=$authState")
        if (isUserLoggedIn && authState is AuthUiState.Success) {
            Log.d("NEXUS_DEBUG", "SplashScreen: Navigating to main")
            onNavigateToMain()
        } else {
            Log.d("NEXUS_DEBUG", "SplashScreen: Navigating to login")
            onNavigateToLogin()
        }
    }

    // Splash UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Blue),  // DEBUG: Changed to bright blue for visibility
        contentAlignment = Alignment.Center
    ) {
        if (!isAnimationComplete) {
            // Fallback: show loading state immediately
            Text(
                text = "Loading...",
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        } else {
            // Animated content
            Column(
                modifier = Modifier
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Nexus Logo (circles animation)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = CircleShape
                            )
                    )

                    // Inner circle
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )

                    // Center dot
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = CircleShape
                            )
                    )
                }

                // App name
                Text(
                    text = "Nexus",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .alpha(alpha)
                        .padding(top = 24.dp)
                )

                // Tagline
                Text(
                    text = "Campus Connected",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .alpha(alpha)
                        .padding(top = 8.dp)
                )
            }
        }
    }
}
