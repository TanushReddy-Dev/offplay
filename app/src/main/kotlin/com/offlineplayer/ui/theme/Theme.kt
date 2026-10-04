package com.offlineplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// -- Brand palette --
private val DeepNavy = Color(0xFF0D1117)
private val DarkSurface = Color(0xFF161B22)
private val DarkSurfaceVariant = Color(0xFF21262D)
private val AccentCoral = Color(0xFFE94560)
private val AccentCoralLight = Color(0xFFFF6B81)
private val AccentBlue = Color(0xFF58A6FF)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val LightBackground = Color(0xFFF6F8FA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFE8ECF0)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCoral,
    onPrimary = Color.White,
    primaryContainer = AccentCoral.copy(alpha = 0.15f),
    onPrimaryContainer = AccentCoralLight,
    secondary = AccentBlue,
    onSecondary = Color.White,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF30363D),
)

private val LightColorScheme = lightColorScheme(
    primary = AccentCoral,
    onPrimary = Color.White,
    primaryContainer = AccentCoral.copy(alpha = 0.1f),
    onPrimaryContainer = Color(0xFF8B1A2B),
    secondary = Color(0xFF0969DA),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1F2328),
    surface = LightSurface,
    onSurface = Color(0xFF1F2328),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF57606A),
    outline = Color(0xFFD0D7DE),
)

@Composable
fun OfflinePlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
