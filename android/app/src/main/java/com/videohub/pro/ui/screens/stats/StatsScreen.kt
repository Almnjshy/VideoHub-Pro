package com.videohub.pro.ui.screens.stats

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess
import com.videohub.pro.utils.StorageHelper
import com.videohub.pro.utils.StorageHelper.formatBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    database: VideoHubDatabase,
) : ViewModel() {
    val stats = database.appStatDao().observe()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val tasks = database.taskDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val statsEntity by viewModel.stats.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val storageInfo = remember { StorageHelper.getStorageInfo(context) }

    val stats = statsEntity
    val totalDownloads = stats?.totalDownloads ?: 0
    val completedDownloads = stats?.completedDownloads ?: 0
    val failedDownloads = stats?.failedDownloads ?: 0
    val totalBytesDownloaded = stats?.totalBytesDownloaded ?: 0L
    val averageSpeed = stats?.averageSpeed ?: 0L
    val successRate = if (totalDownloads > 0) completedDownloads.toFloat() / totalDownloads else 1f

    // Count tasks by platform
    val platformCounts = tasks.groupBy { it.platformId }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
    val maxPlatformCount = platformCounts.maxOfOrNull { it.second } ?: 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("الإحصائيات", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Top stats (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Download,
                label = "إجمالي التنزيلات",
                value = totalDownloads.toString(),
                accent = AmberPrimary,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                label = "مكتملة",
                value = completedDownloads.toString(),
                sub = if (failedDownloads > 0) "$failedDownloads فشل" else "",
                accent = EmeraldSuccess,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                label = "متوسط السرعة",
                value = if (averageSpeed > 0) String.format("%.1f", averageSpeed.toFloat() / (1024 * 1024)) else "0",
                sub = "MB/s",
                accent = Color(0xFF3B82F6),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                label = "معدل النجاح",
                value = "${(successRate * 100).toInt()}%",
                accent = if (successRate > 0.9f) EmeraldSuccess else AmberPrimary,
            )
        }

        // Platform distribution
        Surface(
            color = DarkBgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("توزيع التنزيلات حسب المنصة", color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                if (platformCounts.isEmpty()) {
                    Text("لا توجد بيانات بعد", color = DarkTextSecondary, fontSize = 11.sp)
                } else {
                    platformCounts.take(6).forEach { (platform, count) ->
                        val pct = count.toFloat() / maxPlatformCount
                        val color = when (platform) {
                            "youtube" -> Color(0xFFFF0000)
                            "tiktok" -> Color(0xFFFE2C55)
                            "facebook" -> Color(0xFF1877F2)
                            "instagram" -> Color(0xFFE4405F)
                            "x" -> Color(0xFF888888)
                            else -> AmberPrimary
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(platform, color = DarkTextSecondary, fontSize = 11.sp)
                                Text("$count", color = DarkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(DarkBorder),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(pct)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick stats — REAL values from DB and device
        Surface(
            color = DarkBgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("معلومات إضافية", color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                InfoRow("إجمالي البيانات المنزّلة", formatBytes(totalBytesDownloaded))
                InfoRow("عدد الملفات", storageInfo.fileCount.toString())
                InfoRow("المساحة المستخدمة", "${(storageInfo.usagePercent * 100).toInt()}%")
                InfoRow("مساحة التطبيق", formatBytes(storageInfo.appUsedBytes))
                InfoRow("المساحة المتاحة", formatBytes(storageInfo.availableBytes))
                InfoRow("إجمالي السعة", formatBytes(storageInfo.totalBytes))
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    sub: String = "",
    accent: Color,
) {
    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = DarkTextSecondary, fontSize = 10.sp)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                }
            }
            Text(value, color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (sub.isNotEmpty()) {
                Text(sub, color = DarkTextSecondary, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = DarkTextSecondary, fontSize = 11.sp)
        Text(value, color = DarkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
