package com.videohub.pro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.videohub.pro.data.AppSettings
import com.videohub.pro.i18n.AppStrings

/**
 * Theme Provider — يغلف التطبيق ويوفر الألوان والنصوص ديناميكياً.
 *
 * كل شاشة تستخدم:
 *   val colors = appColors()
 *   val strings = appStrings()
 *   fun s(key: String) = strings[key] ?: key
 *
 * بدلاً من:
 *   DarkBgPrimary, AmberPrimary, "الإعدادات", إلخ
 */
@Composable
fun ThemeProvider(
    appSettings: AppSettings,
    content: @Composable () -> Unit,
) {
    val theme = appSettings.getTheme()
    val accent = appSettings.getAccentIndex()
    val lang = appSettings.getLanguage()
    val colors = remember(theme, accent) { getColors(theme, accent) }
    val strings = remember(lang) { AppStrings.get(lang) }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppStrings provides strings,
    ) {
        content()
    }
}
