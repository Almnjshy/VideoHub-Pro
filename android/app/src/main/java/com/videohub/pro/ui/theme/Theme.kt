package com.videohub.pro.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { DARK, LIGHT, AMOLED }

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = DarkBgPrimary,
    primaryContainer = AmberDark,
    onPrimaryContainer = AmberLight,
    secondary = EmeraldSuccess,
    onSecondary = DarkBgPrimary,
    tertiary = BlueInfo,
    background = DarkBgPrimary,
    onBackground = DarkTextPrimary,
    surface = DarkBgSecondary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBgCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = RedError,
    onError = DarkBgPrimary,
)

private val AmoledColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = AmoledBgPrimary,
    secondary = EmeraldSuccess,
    background = AmoledBgPrimary,
    onBackground = DarkTextPrimary,
    surface = Color(0xFF0A0A0A),
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF1A1A1A),
    error = RedError,
)

private val LightColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = LightBgPrimary,
    secondary = EmeraldSuccess,
    background = LightBgPrimary,
    onBackground = LightTextPrimary,
    surface = LightBgCard,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBgSecondary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = RedError,
)

@Composable
fun VideoHubTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (themeMode == ThemeMode.LIGHT) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
        }
        themeMode == ThemeMode.LIGHT -> LightColorScheme
        themeMode == ThemeMode.AMOLED -> AmoledColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VideoHubTypography,
        content = content,
    )
}
