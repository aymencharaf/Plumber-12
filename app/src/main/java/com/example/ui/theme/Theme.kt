package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = DarkModePrimary,
    onPrimary = DarkModeBackground,
    primaryContainer = DarkModeSurfaceVariant,
    onPrimaryContainer = DarkModePrimary,
    secondary = AquaTealSecondary,
    secondaryContainer = DarkModeSurfaceVariant,
    tertiary = WarmAmberAccent,
    background = DarkModeBackground,
    surface = DarkModeSurface,
    surfaceVariant = DarkModeSurfaceVariant,
    onBackground = DarkModeOnSurface,
    onSurface = DarkModeOnSurface,
    onSurfaceVariant = DarkModeMutedText,
    outline = DarkModeOutline,
    outlineVariant = DarkModeOutline
)

private val LightColorScheme = lightColorScheme(
    primary = PlumberBluePrimary,
    onPrimary = Color.White,
    primaryContainer = LightBlueContainer,
    onPrimaryContainer = OceanBlueDark,
    secondary = AquaTealSecondary,
    secondaryContainer = LightBlueVariant,
    tertiary = WarmAmberAccent,
    background = PureWhiteBackground,
    surface = PureWhiteSurface,
    surfaceVariant = LightBlueContainer,
    onBackground = DarkSlateText,
    onSurface = DarkSlateText,
    onSurfaceVariant = MutedSlateText,
    outline = LightBorderOutline,
    outlineVariant = LightBorderOutline
)

@Composable
fun PlumberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent brand PPR green
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    PlumberTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

