package com.example.nexus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Nexus Light Color Scheme
 * Primary: Deep Indigo, Secondary: Teal
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryIndigoLight,
    onPrimaryContainer = PrimaryIndigoDark,
    
    secondary = SecondaryTeal,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = SecondaryTealLight,
    onSecondaryContainer = SecondaryTealDark,
    
    tertiary = IntentAskingColor,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    
    error = ErrorRed,
    onError = androidx.compose.ui.graphics.Color.White
)

/**
 * Nexus Dark Color Scheme
 * Primary: Deep Indigo, Secondary: Teal
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigoLight,
    onPrimary = PrimaryIndigoDark,
    primaryContainer = PrimaryIndigo,
    onPrimaryContainer = PrimaryIndigoLight,
    
    secondary = SecondaryTealLight,
    onSecondary = SecondaryTealDark,
    secondaryContainer = SecondaryTeal,
    onSecondaryContainer = SecondaryTealLight,
    
    tertiary = IntentAskingColor,
    onTertiary = PrimaryIndigoDark,
    
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    
    error = ErrorRed,
    onError = PrimaryIndigoDark
)

/**
 * Nexus Theme Composable
 * Supports Material Design 3, dynamic color (Android 12+), and dark mode
 */
@Composable
fun NexusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexusTypography,
        content = content
    )
}