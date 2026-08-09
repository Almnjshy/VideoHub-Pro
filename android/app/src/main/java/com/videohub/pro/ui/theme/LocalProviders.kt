package com.videohub.pro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentComposer
import com.videohub.pro.i18n.AppStrings

// LocalAppColors is already defined in AppColors.kt — do NOT redeclare it here.
val LocalAppStrings = compositionLocalOf { AppStrings.get("ar") }

@Composable
@ReadOnlyComposable
fun appColors(): AppColors = LocalAppColors.current

@Composable
@ReadOnlyComposable
fun appStrings(): Map<String, String> = LocalAppStrings.current
