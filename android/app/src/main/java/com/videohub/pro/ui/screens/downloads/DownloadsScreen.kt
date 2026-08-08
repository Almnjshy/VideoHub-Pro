package com.videohub.pro.ui.screens.downloads

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.data.database.entities.TaskEntity
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess
import com.videohub.pro.ui.theme.RedError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    database: VideoHubDatabase,
) : ViewModel() {
    val tasks = database.taskDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

enum class DownloadFilter(val label: String, val statuses: Set<String>) {
    ALL("الكل", emptySet()),
    ACTIVE("النشطة", setOf("queued", "downloading", "paused", "retrying")),
    COMPLETED("المكتملة", setOf("completed")),
    FAILED("الفاشلة", setOf("failed")),
}

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onPlayFile: (filePath: String, title: String, isVideo: Boolean) -> Unit = { _, _, _ -> },
) {
    val tasks by viewModel.tasks.collectAsState()
    var filter by remember { mutableStateOf(DownloadFilter.ALL) }

    val filtered = if (filter == DownloadFilter.ALL) tasks
    else tasks.filter { it.status in filter.statuses }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("التنزيلات", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.FilterList, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(20.dp))
        }

        // Filter tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DownloadFilter.entries.forEach { f ->
                val count = if (f == DownloadFilter.ALL) tasks.size
                    else tasks.count { it.status in f.statuses }
                val selected = filter == f
                Surface(
                    color = if (selected) AmberPrimary.copy(alpha = 0.15f) else DarkBgCard,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(f.label, color = if (selected) AmberPrimary else DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(count.toString(), color = if (selected) AmberPrimary else DarkBorder, fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Task list
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📭", fontSize = 32.sp)
                    Text("لا توجد مهام", color = DarkTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { task ->
                    TaskItem(task, onPlayFile)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: TaskEntity,
    onPlayFile: (filePath: String, title: String, isVideo: Boolean) -> Unit,
) {
    val statusColor = when (task.status) {
        "downloading" -> EmeraldSuccess
        "completed" -> Color(0xFF22C55E)
        "failed" -> RedError
        "paused" -> DarkTextSecondary
        else -> AmberPrimary
    }
    val statusLabel = when (task.status) {
        "downloading" -> "قيد التنزيل"
        "completed" -> "مكتمل"
        "failed" -> "فشل"
        "paused" -> "متوقف"
        "queued" -> "في الانتظار"
        "retrying" -> "إعادة المحاولة"
        else -> task.status
    }

    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Open media player if file exists
                if (task.status == "completed" && task.outputPath != null) {
                    val file = java.io.File(task.outputPath)
                    if (file.exists()) {
                        onPlayFile(task.outputPath, task.title, task.formatMediaType == "video")
                    }
                }
            },
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(task.title, color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(statusLabel, color = statusColor, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(
                "${task.platformId} · ${task.formatQuality} · ${task.totalBytes / (1024 * 1024)} MB",
                color = DarkTextSecondary,
                fontSize = 10.sp,
            )
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = statusColor,
                trackColor = DarkBorder,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${(task.progress * 100).toInt()}%", color = DarkTextSecondary, fontSize = 10.sp)
                if (task.status == "downloading") {
                    Text("${task.speedBps / (1024 * 1024)} MB/s", color = EmeraldSuccess, fontSize = 10.sp)
                }
                if (task.status == "completed") {
                    Text("▶ تشغيل", color = AmberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
