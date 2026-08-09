package com.videohub.pro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * App Colors — تتغير ديناميكياً حسب إعدادات المستخدم.
 */
data class AppColors(
    val bgPrimary: Color,
    val bgCard: Color,
    val bgSecondary: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color,
    val success: Color,
    val error: Color,
    val info: Color,
    val warning: Color,
)

val DarkColors = AppColors(
    bgPrimary = Color(0xFF09090B),
    bgCard = Color(0xFF18181B),
    bgSecondary = Color(0xFF18181B),
    border = Color(0xFF27272A),
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF71717A),
    accent = Color(0xFFF59E0B),
    accentLight = Color(0xFFFCD34D),
    accentDark = Color(0xFFD97706),
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    info = Color(0xFF3B82F6),
    warning = Color(0xFFF97316),
)

val LightColors = AppColors(
    bgPrimary = Color(0xFFFAFAFA),
    bgCard = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFF4F4F5),
    border = Color(0xFFE4E4E7),
    textPrimary = Color(0xFF18181B),
    textSecondary = Color(0xFF71717A),
    textTertiary = Color(0xFFA1A1AA),
    accent = Color(0xFFF59E0B),
    accentLight = Color(0xFFFCD34D),
    accentDark = Color(0xFFD97706),
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    info = Color(0xFF3B82F6),
    warning = Color(0xFFF97316),
)

val AmoledColors = AppColors(
    bgPrimary = Color(0xFF000000),
    bgCard = Color(0xFF0A0A0A),
    bgSecondary = Color(0xFF0A0A0A),
    border = Color(0xFF1A1A1A),
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF71717A),
    accent = Color(0xFFF59E0B),
    accentLight = Color(0xFFFCD34D),
    accentDark = Color(0xFFD97706),
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    info = Color(0xFF3B82F6),
    warning = Color(0xFFF97316),
)

/** Accent color palettes — 6 options */
val AccentColors = listOf(
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFFEF4444), // Red
)

val AccentColorsLight = listOf(
    Color(0xFFFCD34D),
    Color(0xFF34D399),
    Color(0xFF60A5FA),
    Color(0xFFA78BFA),
    Color(0xFFF472B6),
    Color(0xFFF87171),
)

val AccentColorsDark = listOf(
    Color(0xFFD97706),
    Color(0xFF059669),
    Color(0xFF2563EB),
    Color(0xFF7C3AED),
    Color(0xFFDB2777),
    Color(0xFFDC2626),
)

/**
 * Get colors for a specific theme + accent combination.
 */
fun getColors(theme: String, accentIndex: Int): AppColors {
    val base = when (theme) {
        "light" -> LightColors
        "amoled" -> AmoledColors
        else -> DarkColors
    }
    val accent = AccentColors.getOrElse(accentIndex) { AccentColors[0] }
    val accentLight = AccentColorsLight.getOrElse(accentIndex) { AccentColorsLight[0] }
    val accentDark = AccentColorsDark.getOrElse(accentIndex) { AccentColorsDark[0] }

    return base.copy(accent = accent, accentLight = accentLight, accentDark = accentDark)
}

val LocalAppColors = staticCompositionLocalOf { DarkColors }
