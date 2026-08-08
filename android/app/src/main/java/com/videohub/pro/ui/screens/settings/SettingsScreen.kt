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
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess

@Composable
fun SettingsScreen() {
    var selectedQuality by remember { mutableStateOf("1080p") }
    var concurrentDownloads by remember { mutableStateOf(3) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoRetry by remember { mutableStateOf(true) }
    var selectedTheme by remember { mutableStateOf("dark") }
    var selectedAccent by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("الإعدادات", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Default Quality
        SettingsSection(icon = Icons.Default.HighQuality, title = "الجودة الافتراضية") {
            val qualities = listOf("360p", "480p", "720p", "1080p", "4k", "audio")
            qualities.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.forEach { q ->
                        val selected = selectedQuality == q
                        Surface(
                            color = if (selected) AmberPrimary.copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedQuality = q },
                        ) {
                            Text(
                                if (q == "audio") "صوت" else q,
                                color = if (selected) AmberPrimary else DarkTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                    if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // Concurrency
        SettingsSection(icon = Icons.Default.Speed, title = "التنزيلات المتوازية") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("عدد التنزيلات المتزامنة", color = DarkTextSecondary, fontSize = 12.sp)
                Text(concurrentDownloads.toString(), color = AmberPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (1..6).forEach { n ->
                    val selected = concurrentDownloads == n
                    Surface(
                        color = if (selected) AmberPrimary else DarkBorder.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { concurrentDownloads = n },
                    ) {
                        Text(
                            n.toString(),
                            color = if (selected) Color.Black else DarkTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Storage
        SettingsSection(icon = Icons.Default.Storage, title = "التخزين") {
            InfoRow("مسار التنزيل", "/storage/VideoHub/")
            InfoRow("المساحة المستخدمة", "11.8 GB / 32 GB")
            InfoRow("عدد الملفات", "41 ملف")
        }

        // Notifications
        SettingsSection(icon = Icons.Default.Notifications, title = "الإشعارات") {
            ToggleRow(
                label = "إشعارات النظام",
                description = "إظهار تقدم التنزيل في الإشعارات",
                checked = notificationsEnabled,
                onToggle = { notificationsEnabled = it },
            )
        }

        // Auto-retry
        SettingsSection(icon = Icons.Default.Speed, title = "إعادة المحاولة") {
            ToggleRow(
                label = "إعادة المحاولة التلقائية",
                description = "إعادة محاولة المهام الفاشلة تلقائياً",
                checked = autoRetry,
                onToggle = { autoRetry = it },
            )
        }

        // Theme
        SettingsSection(icon = Icons.Default.Palette, title = "المظهر") {
            val themes = listOf("dark" to "داكنة", "light" to "فاتحة", "amoled" to "AMOLED")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                themes.forEach { (id, label) ->
                    val selected = selectedTheme == id
                    Surface(
                        color = if (selected) AmberPrimary.copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTheme = id },
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (id) {
                                            "dark" -> Color(0xFF18181B)
                                            "light" -> Color(0xFFFAFAFA)
                                            "amoled" -> Color.Black
                                            else -> Color.Gray
                                        },
                                    ),
                            )
                            Text(label, color = if (selected) AmberPrimary else DarkTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Accent colors
            Text("اللون المميز", color = DarkTextSecondary, fontSize = 11.sp)
            val accents = listOf(
                Color(0xFFF59E0B),
                Color(0xFF10B981),
                Color(0xFF3B82F6),
                Color(0xFF8B5CF6),
                Color(0xFFEC4899),
                Color(0xFFEF4444),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                accents.forEachIndexed { idx, color ->
                    val selected = selectedAccent == idx
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color)
                            .clickable { selectedAccent = idx },
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }

        // Language
        SettingsSection(icon = Icons.Default.Language, title = "اللغة") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("ar" to "العربية", "en" to "English").forEach { (id, label) ->
                    Surface(
                        color = DarkBorder.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { },
                    ) {
                        Text(
                            label,
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }

        // About
        SettingsSection(icon = Icons.Default.HighQuality, title = "حول التطبيق") {
            InfoRow("الإصدار", "3.2.0 (Native)")
            InfoRow("محرك التنزيل", "Foreground Service + Room")
            InfoRow("الوحدات", "14 منصة مدعومة")
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(16.dp))
                }
                Text(title, color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = DarkTextSecondary, fontSize = 12.sp)
        Text(value, color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DiagnosticsButton(onClick: () -> Unit) {
    Surface(
        color = AmberPrimary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmberPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.HighQuality, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Resolver Diagnostics", color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Test platform resolvers with real URLs", color = DarkTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = DarkTextSecondary, fontSize = 10.sp)
        }
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) EmeraldSuccess else DarkBorder)
                .clickable { onToggle(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
            )
        }
    }
}
