package com.videohub.pro.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.videohub.pro.data.AppSettings
import com.videohub.pro.i18n.AppStrings
import com.videohub.pro.resolver.ResolverManager
import com.videohub.pro.ui.theme.AccentColors
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess
import com.videohub.pro.utils.StorageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val resolverManager: ResolverManager,
    val appSettings: AppSettings,
) : ViewModel()

enum class HealthState { Unknown, Checking, Connected, Failed }

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = viewModel.appSettings.getLanguage()
    val strings = remember(lang) { AppStrings.get(lang) }
    fun s(key: String) = strings[key] ?: key

    // Read ALL settings from AppSettings (saved in SharedPreferences)
    var selectedQuality by remember { mutableStateOf(viewModel.appSettings.getDefaultQuality()) }
    var concurrentDownloads by remember { mutableIntStateOf(viewModel.appSettings.getConcurrentDownloads()) }
    var notificationsEnabled by remember { mutableStateOf(viewModel.appSettings.getNotificationsEnabled()) }
    var autoRetry by remember { mutableStateOf(viewModel.appSettings.getAutoRetry()) }
    var selectedTheme by remember { mutableStateOf(viewModel.appSettings.getTheme()) }
    var selectedAccent by remember { mutableIntStateOf(viewModel.appSettings.getAccentIndex()) }
    var selectedLanguage by remember { mutableStateOf(viewModel.appSettings.getLanguage()) }
    var healthStatus by remember { mutableStateOf(HealthState.Unknown) }

    // Storage info
    val storageInfo = remember { StorageHelper.getStorageInfo(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(s("settings"), color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // ============ yt-dlp Engine ============
        SettingsSection(icon = Icons.Default.Cloud, title = s("engine_yt_dlp")) {
            Text(s("engine_embedded"), color = DarkTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        healthStatus = HealthState.Checking
                        scope.launch {
                            val ready = withContext(Dispatchers.IO) { viewModel.resolverManager.isReady() }
                            healthStatus = if (ready) HealthState.Connected else HealthState.Failed
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColors[selectedAccent]),
                ) { Text(s("check_engine"), color = Color.Black, fontSize = 12.sp) }
                when (healthStatus) {
                    HealthState.Connected -> Text("✓ ${s("ready")} (yt-dlp ${viewModel.resolverManager.getVersion()})", color = EmeraldSuccess, fontSize = 12.sp)
                    HealthState.Failed -> Text("✗ ${s("not_ready")}", color = Color(0xFFEF4444), fontSize = 12.sp)
                    HealthState.Checking -> Text(s("checking"), color = DarkTextSecondary, fontSize = 12.sp)
                    HealthState.Unknown -> Text(s("press_to_check"), color = DarkTextSecondary, fontSize = 12.sp)
                }
            }
        }

        // ============ Default Quality ============
        SettingsSection(icon = Icons.Default.HighQuality, title = s("default_quality")) {
            val qualities = listOf("360p", "480p", "720p", "1080p", "4k", "audio")
            qualities.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { q ->
                        val selected = selectedQuality == q
                        Surface(
                            color = if (selected) AccentColors[selectedAccent].copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable {
                                selectedQuality = q
                                viewModel.appSettings.setDefaultQuality(q) // ← SAVE
                            },
                        ) {
                            Text(
                                if (q == "audio") s("audio") else q,
                                color = if (selected) AccentColors[selectedAccent] else DarkTextSecondary,
                                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center,
                            )
                        }
                    }
                    if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // ============ Concurrent Downloads ============
        SettingsSection(icon = Icons.Default.Speed, title = s("concurrent_downloads")) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(s("concurrent_downloads"), color = DarkTextSecondary, fontSize = 12.sp)
                Text(concurrentDownloads.toString(), color = AccentColors[selectedAccent], fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..6).forEach { n ->
                    val selected = concurrentDownloads == n
                    Surface(
                        color = if (selected) AccentColors[selectedAccent] else DarkBorder.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).clickable {
                            concurrentDownloads = n
                            viewModel.appSettings.setConcurrentDownloads(n) // ← SAVE
                        },
                    ) {
                        Text(n.toString(), color = if (selected) Color.Black else DarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // ============ Storage (REAL data) ============
        SettingsSection(icon = Icons.Default.Storage, title = s("storage")) {
            InfoRow(s("download_path"), "/storage/VideoHub/")
            InfoRow(s("used_space"), "${StorageHelper.formatBytes(storageInfo.partitionUsedBytes)} / ${StorageHelper.formatBytes(storageInfo.totalBytes)}")
            InfoRow(s("app_storage"), StorageHelper.formatBytes(storageInfo.appUsedBytes))
            InfoRow(s("available_space"), StorageHelper.formatBytes(storageInfo.availableBytes))
            InfoRow(s("file_count"), "${storageInfo.fileCount}")
        }

        // ============ Notifications ============
        SettingsSection(icon = Icons.Default.Notifications, title = s("notifications_section")) {
            ToggleRow(
                label = s("system_notifications"),
                description = s("show_download_progress"),
                checked = notificationsEnabled,
                onToggle = {
                    notificationsEnabled = it
                    viewModel.appSettings.setNotificationsEnabled(it) // ← SAVE
                },
            )
        }

        // ============ Auto Retry ============
        SettingsSection(icon = Icons.Default.Speed, title = s("auto_retry")) {
            ToggleRow(
                label = s("auto_retry"),
                description = s("retry_failed_automatically"),
                checked = autoRetry,
                onToggle = {
                    autoRetry = it
                    viewModel.appSettings.setAutoRetry(it) // ← SAVE
                },
            )
        }

        // ============ Theme ============
        SettingsSection(icon = Icons.Default.Palette, title = s("appearance")) {
            val themes = listOf("dark" to s("dark"), "light" to s("light"), "amoled" to s("amoled"))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                themes.forEach { (id, label) ->
                    val selected = selectedTheme == id
                    Surface(
                        color = if (selected) AccentColors[selectedAccent].copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable {
                            selectedTheme = id
                            viewModel.appSettings.setTheme(id) // ← SAVE
                        },
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(
                                when (id) { "dark" -> Color(0xFF18181B); "light" -> Color(0xFFFAFAFA); "amoled" -> Color.Black; else -> Color.Gray }
                            ))
                            Text(label, color = if (selected) AccentColors[selectedAccent] else DarkTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Accent colors
            Text(s("accent_color"), color = DarkTextSecondary, fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentColors.forEachIndexed { idx, color ->
                    val selected = selectedAccent == idx
                    Box(
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(color).clickable {
                            selectedAccent = idx
                            viewModel.appSettings.setAccentIndex(idx) // ← SAVE
                        },
                    ) {
                        if (selected) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                    }
                }
            }
        }

        // ============ Language ============
        SettingsSection(icon = Icons.Default.Language, title = s("language")) {
            val allLangs = com.videohub.pro.i18n.AppStrings.supportedLanguages
            allLangs.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (id, label) ->
                        val selected = selectedLanguage == id
                        Surface(
                            color = if (selected) AccentColors[selectedAccent].copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable {
                                selectedLanguage = id
                                viewModel.appSettings.setLanguage(id)
                            },
                        ) {
                            Text(label, color = if (selected) AccentColors[selectedAccent] else DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center)
                        }
                    }
                    if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // ============ About ============
        SettingsSection(icon = Icons.Default.HighQuality, title = s("about")) {
            InfoRow(s("version"), "3.2.0 (Native)")
            InfoRow(s("download_engine"), "Foreground Service + Room + Chaquopy 17")
            InfoRow(s("supported_platforms"), "14+ (yt-dlp: 1000+)")
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SettingsSection(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Surface(color = DarkBgCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(AccentColors[0].copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = AccentColors[0], modifier = Modifier.size(16.dp))
                }
                Text(title, color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DarkTextSecondary, fontSize = 12.sp)
        Text(value, color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ToggleRow(label: String, description: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = DarkTextSecondary, fontSize = 10.sp)
        }
        Box(
            modifier = Modifier.size(width = 44.dp, height = 24.dp).clip(RoundedCornerShape(12.dp))
                .background(if (checked) EmeraldSuccess else DarkBorder).clickable { onToggle(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(modifier = Modifier.padding(horizontal = 2.dp).size(20.dp).clip(RoundedCornerShape(10.dp)).background(Color.White))
        }
    }
}
