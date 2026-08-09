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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.videohub.pro.utils.StorageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val database: VideoHubDatabase,
) : ViewModel() {
    val tasks = database.taskDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun deleteTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.taskDao().delete(taskId)
        }
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = database.taskDao().getById(taskId) ?: return@launch
            database.taskDao().update(
                task.copy(
                    status = "queued",
                    progress = 0f,
                    downloadedBytes = 0L,
                    error = null,
                    errorStage = null,
                )
            )
        }
    }

    fun pauseTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = database.taskDao().getById(taskId) ?: return@launch
            database.taskDao().update(task.copy(status = "paused"))
        }
    }

    fun resumeTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = database.taskDao().getById(taskId) ?: return@launch
            database.taskDao().update(task.copy(status = "queued"))
        }
    }
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
    onOpenWorkspace: (TaskEntity) -> Unit = {},
) {
    val tasks by viewModel.tasks.collectAsState()
    var filter by remember { mutableStateOf(DownloadFilter.ALL) }
    var deleteTarget by remember { mutableStateOf<TaskEntity?>(null) }

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
                    TaskItem(
                        task = task,
                        onPlay = {
                            if (task.outputPath != null) {
                                val file = File(task.outputPath)
                                if (file.exists()) {
                                    onPlayFile(task.outputPath, task.title, task.formatMediaType == "video")
                                }
                            }
                        },
                        onOpen = {
                            if (task.outputPath != null) {
                                val file = File(task.outputPath)
                                if (file.exists()) {
                                    onPlayFile(task.outputPath, task.title, task.formatMediaType == "video")
                                }
                            }
                        },
                        onWorkspace = { onOpenWorkspace(task) },
                        onDelete = { deleteTarget = task },
                        onRetry = { viewModel.retryTask(task.id) },
                        onPause = { viewModel.pauseTask(task.id) },
                        onResume = { viewModel.resumeTask(task.id) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف المهمة", color = DarkTextPrimary) },
            text = { Text("هل تريد حذف \"${task.title}\"؟", color = DarkTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task.id)
                    deleteTarget = null
                }) { Text("حذف", color = RedError) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("إلغاء", color = DarkTextSecondary) }
            },
        )
    }
}

@Composable
private fun TaskItem(
    task: TaskEntity,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    onWorkspace: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
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

    val priorityLabel = when (task.priority) {
        0 -> "عالية"
        2 -> "منخفضة"
        else -> "عادية"
    }
    val priorityColor = when (task.priority) {
        0 -> RedError
        2 -> DarkTextSecondary
        else -> AmberPrimary
    }

    val dateFormat = remember { SimpleDateFormat("h:mm a", Locale("ar")) }

    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Title + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    task.title,
                    color = DarkTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            // Details: priority · size · format · platform
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Priority badge
                Surface(color = priorityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        priorityLabel,
                        color = priorityColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
                Text(
                    "${StorageHelper.formatBytesShort(task.totalBytes)}",
                    color = DarkTextSecondary,
                    fontSize = 9.sp,
                )
                Text(
                    "· ${task.formatQuality.uppercase()} • ${task.formatExt.uppercase()}",
                    color = DarkTextSecondary,
                    fontSize = 9.sp,
                )
                Text(
                    "· ${task.platformId.uppercase()}",
                    color = DarkTextSecondary,
                    fontSize = 9.sp,
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = statusColor,
                trackColor = DarkBorder,
            )

            // Progress info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: timestamp or speed
                if (task.status == "completed" && task.completedAt != null) {
                    Text(
                        dateFormat.format(Date(task.completedAt)),
                        color = DarkTextSecondary,
                        fontSize = 9.sp,
                    )
                } else if (task.status == "downloading") {
                    Text(
                        "${StorageHelper.formatBytesShort(task.speedBps)}/s",
                        color = EmeraldSuccess,
                        fontSize = 9.sp,
                    )
                } else {
                    Text(
                        "${(task.progress * 100).toInt()}%",
                        color = DarkTextSecondary,
                        fontSize = 9.sp,
                    )
                }

                // Right: downloaded / total
                Text(
                    "${StorageHelper.formatBytesShort(task.downloadedBytes)} / ${StorageHelper.formatBytesShort(task.totalBytes)}",
                    color = DarkTextSecondary,
                    fontSize = 9.sp,
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Delete button
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "حذف",
                    color = RedError,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )

                // Open / Resume / Retry button
                when (task.status) {
                    "completed" -> ActionButton(
                        icon = Icons.Default.FolderOpen,
                        label = "فتح",
                        color = EmeraldSuccess,
                        onClick = onOpen,
                        modifier = Modifier.weight(1f),
                    )
                    "paused" -> ActionButton(
                        icon = Icons.Default.PlayArrow,
                        label = "متابعة",
                        color = AmberPrimary,
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                    )
                    "failed" -> ActionButton(
                        icon = Icons.Default.Refresh,
                        label = "إعادة",
                        color = AmberPrimary,
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                    )
                    "downloading" -> ActionButton(
                        icon = Icons.Default.Pause,
                        label = "إيقاف",
                        color = DarkTextSecondary,
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                    )
                    else -> ActionButton(
                        icon = Icons.Default.PlayArrow,
                        label = "انتظار",
                        color = DarkTextSecondary,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        enabled = false,
                    )
                }

                // Workspace button
                ActionButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "مساحة عمل",
                    color = AmberPrimary,
                    onClick = onWorkspace,
                    modifier = Modifier.weight(1f),
                )

                // Play button (only for completed)
                if (task.status == "completed" && task.outputPath != null) {
                    ActionButton(
                        icon = Icons.Default.PlayArrow,
                        label = "تشغيل",
                        color = EmeraldSuccess,
                        onClick = onPlay,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        color = if (enabled) color.copy(alpha = 0.1f) else DarkBorder.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) color else DarkBorder, modifier = Modifier.size(12.dp))
            Spacer(Modifier.size(2.dp))
            Text(label, color = if (enabled) color else DarkBorder, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        }
    }
}
