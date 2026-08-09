package com.videohub.pro.ui.screens.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.ui.navigation.Screen
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBgSecondary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess
import com.videohub.pro.ui.theme.RedError
import com.videohub.pro.utils.StorageHelper
import com.videohub.pro.utils.StorageHelper.formatBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    database: VideoHubDatabase,
) : ViewModel() {
    val tasks = database.taskDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val notifications = database.notificationDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    onShareUrl: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val activeTasks = tasks.filter {
        it.status == "downloading" || it.status == "queued" || it.status == "retrying"
    }
    val completedTasks = tasks.filter { it.status == "completed" }
    val failedTasks = tasks.filter { it.status == "failed" }
    val unreadNotifications = notifications.count { !it.read }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "مرحباً بك",
                    color = DarkTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (activeTasks.isNotEmpty()) "${activeTasks.size} تنزيل نشط"
                    else "لا توجد تنزيلات نشطة",
                    color = DarkTextSecondary,
                    fontSize = 12.sp,
                )
            }
            Surface(
                color = EmeraldSuccess.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldSuccess),
                    )
                    Text("المحرك يعمل", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Smart Download Card — Primary feature on Home
        SmartDownloadCard(onShareUrl = onShareUrl)

        Spacer(Modifier.height(4.dp))

        // KPI Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "نشطة",
                value = activeTasks.size.toString(),
                sub = "${tasks.size} إجمالي",
                accent = AmberPrimary,
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "مكتملة",
                value = completedTasks.size.toString(),
                sub = "${failedTasks.size} فشل",
                accent = EmeraldSuccess,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "إشعارات",
                value = unreadNotifications.toString(),
                sub = "غير مقروء",
                accent = AmberPrimary,
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                label = "إجمالي",
                value = tasks.size.toString(),
                sub = "مهمة",
                accent = DarkTextSecondary,
            )
        }

        // Quick Actions (3 columns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LibraryMusic,
                label = "مكتبتي",
                accent = EmeraldSuccess,
                onClick = { onNavigate(Screen.Library) },
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Explore,
                label = "اكتشف",
                accent = AmberPrimary,
                onClick = { onNavigate(Screen.Discover) },
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Search,
                label = "بحث",
                accent = Color(0xFF3B82F6),
                onClick = { onNavigate(Screen.Search) },
            )
        }

        // Storage card — uses REAL device storage via StatFs
        val storageInfo = remember { StorageHelper.getStorageInfo(context) }
        Surface(
            color = DarkBgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(18.dp))
                        Text("التخزين", color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("${storageInfo.fileCount} ملف", color = DarkTextSecondary, fontSize = 10.sp)
                }
                LinearProgressIndicator(
                    progress = { storageInfo.usagePercent.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (storageInfo.usagePercent > 0.9f) RedError else AmberPrimary,
                    trackColor = DarkBorder,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatBytes(storageInfo.partitionUsedBytes), color = DarkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text("/ ${formatBytes(storageInfo.totalBytes)}", color = DarkTextSecondary, fontSize = 11.sp)
                }
                Text(
                    "مساحة التطبيق: ${formatBytes(storageInfo.appUsedBytes)} · متاح: ${formatBytes(storageInfo.availableBytes)}",
                    color = DarkTextSecondary,
                    fontSize = 9.sp,
                )
            }
        }

        // Active Downloads
        Text(
            "التنزيلات النشطة",
            color = DarkTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )

        if (activeTasks.isEmpty()) {
            Surface(
                color = DarkBgCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = DarkBorder, modifier = Modifier.size(32.dp))
                    Text("لا توجد تنزيلات نشطة", color = DarkTextSecondary, fontSize = 13.sp)
                    Text("استخدم محاكي المشاركة لبدء تنزيل جديد", color = DarkBorder, fontSize = 10.sp)
                }
            }
        } else {
            activeTasks.take(3).forEach { task ->
                TaskRow(
                    title = task.title,
                    platform = task.platformId,
                    progress = task.progress,
                    status = task.status,
                    speed = "${task.speedBps / (1024 * 1024)} MB/s",
                )
            }
        }

        // Recent activity
        if (tasks.isNotEmpty()) {
            Text(
                "آخر العمليات",
                color = DarkTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            tasks.take(3).forEach { task ->
                TaskRow(
                    title = task.title,
                    platform = task.platformId,
                    progress = task.progress,
                    status = task.status,
                    speed = if (task.status == "downloading") "${task.speedBps / (1024 * 1024)} MB/s" else "",
                )
            }
        }

        // Diagnostics card
        Surface(
            color = DarkBgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.Notifications) },
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
                        .background(if (failedTasks.isNotEmpty()) RedError.copy(alpha = 0.15f) else AmberPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (failedTasks.isNotEmpty()) Icons.Default.Warning else Icons.Default.Compress,
                        contentDescription = null,
                        tint = if (failedTasks.isNotEmpty()) RedError else AmberPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("الإشعارات والأعطال", color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (failedTasks.isNotEmpty()) "${failedTasks.size} مهمة فاشلة بحاجة لانتباه"
                        else "كل الأنظمة تعمل بسلاسة",
                        color = DarkTextSecondary,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp)) // bottom nav padding
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String,
    accent: Color,
) {
    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = DarkTextSecondary, fontSize = 10.sp)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.15f)),
                )
            }
            Text(value, color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = DarkTextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(label, color = DarkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TaskRow(
    title: String,
    platform: String,
    progress: Float,
    status: String,
    speed: String,
) {
    val statusColor = when (status) {
        "downloading" -> EmeraldSuccess
        "completed" -> Color(0xFF22C55E)
        "failed" -> RedError
        "paused" -> DarkTextSecondary
        else -> AmberPrimary
    }
    val statusLabel = when (status) {
        "downloading" -> "قيد التنزيل"
        "completed" -> "مكتمل"
        "failed" -> "فشل"
        "paused" -> "متوقف"
        "queued" -> "في الانتظار"
        "retrying" -> "إعادة المحاولة"
        else -> status
    }

    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(statusLabel, color = statusColor, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text("$platform · $speed", color = DarkTextSecondary, fontSize = 10.sp)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = statusColor,
                trackColor = DarkBorder,
            )
        }
    }
}

@Composable
private fun SmartDownloadCard(onShareUrl: (String) -> Unit) {
    var url by remember { mutableStateOf("") }

    Surface(
        color = AmberPrimary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text("تنزيل ذكي", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("الصق رابط فيديو من أي تطبيق", color = DarkTextSecondary, fontSize = 11.sp)
                }
            }

            // URL input
            Surface(
                color = DarkBgPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(16.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = url,
                        onValueChange = { url = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = DarkTextPrimary,
                            fontSize = 13.sp,
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(AmberPrimary),
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            if (url.isEmpty()) {
                                Text("الصق رابط الفيديو هنا...", color = DarkBorder, fontSize = 13.sp)
                            }
                            innerTextField()
                        },
                    )
                    if (url.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(14.dp).clickable { url = "" },
                        )
                    }
                }
            }

            // Action button
            androidx.compose.material3.Button(
                onClick = {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        onShareUrl(url)
                        url = ""
                    }
                },
                enabled = url.startsWith("http://") || url.startsWith("https://"),
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = AmberPrimary,
                    disabledContainerColor = DarkBorder,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text("تحليل وتنزيل", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            // Quick platform hints
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("YouTube", "TikTok", "Facebook", "Instagram").forEach { platform ->
                    Surface(
                        color = DarkBorder.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            platform,
                            color = DarkTextSecondary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
