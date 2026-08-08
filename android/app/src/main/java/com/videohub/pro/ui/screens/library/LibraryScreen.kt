package com.videohub.pro.ui.screens.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    database: VideoHubDatabase,
) : ViewModel() {
    val tasks = database.taskDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

enum class LibraryCategory(val label: String, val icon: ImageVector) {
    ALL("الكل", Icons.Default.Folder),
    VIDEO("فيديو", Icons.Default.Movie),
    AUDIO("صوت", Icons.Default.MusicNote),
    IMAGE("صور", Icons.Default.Image),
    FAVORITES("المفضلة", Icons.Default.Star),
    RECENT("حديثة", Icons.Default.Schedule),
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsState()
    var category by remember { mutableStateOf(LibraryCategory.ALL) }

    val completed = tasks.filter { it.status == "completed" }
    val filtered = when (category) {
        LibraryCategory.ALL -> completed
        LibraryCategory.VIDEO -> completed.filter { it.formatMediaType == "video" }
        LibraryCategory.AUDIO -> completed.filter { it.formatMediaType == "audio" }
        LibraryCategory.IMAGE -> completed.filter { it.formatMediaType == "image" }
        LibraryCategory.FAVORITES -> emptyList() // would need favorites persistence
        LibraryCategory.RECENT -> completed.sortedByDescending { it.completedAt ?: it.createdAt }.take(10)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBgPrimary),
    ) {
        // Header
        Text(
            "مكتبة الوسائط",
            color = DarkTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        // Categories
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LibraryCategory.entries.forEach { cat ->
                val count = when (cat) {
                    LibraryCategory.ALL -> completed.size
                    LibraryCategory.VIDEO -> completed.count { it.formatMediaType == "video" }
                    LibraryCategory.AUDIO -> completed.count { it.formatMediaType == "audio" }
                    LibraryCategory.IMAGE -> completed.count { it.formatMediaType == "image" }
                    LibraryCategory.FAVORITES -> 0
                    LibraryCategory.RECENT -> completed.size
                }
                val selected = category == cat
                Surface(
                    color = if (selected) AmberPrimary.copy(alpha = 0.15f) else DarkBgCard,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(cat.icon, contentDescription = null, tint = if (selected) AmberPrimary else DarkTextSecondary, modifier = Modifier.size(14.dp))
                        Text(cat.label, color = if (selected) AmberPrimary else DarkTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text(count.toString(), color = if (selected) AmberPrimary else DarkBorder, fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Grid
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📁", fontSize = 32.sp)
                    Text("لا توجد ملفات بعد", color = DarkTextSecondary, fontSize = 13.sp)
                    Text("ستظهر ملفاتك المكتملة هنا", color = DarkBorder, fontSize = 10.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { task ->
                    MediaCard(task)
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun MediaCard(task: TaskEntity) {
    val typeIcon = when (task.formatMediaType) {
        "video" -> Icons.Default.Movie
        "audio" -> Icons.Default.MusicNote
        "image" -> Icons.Default.Image
        else -> Icons.Default.Folder
    }
    val typeColor = when (task.formatMediaType) {
        "video" -> AmberPrimary
        "audio" -> Color(0xFF3B82F6)
        "image" -> EmeraldSuccess
        else -> DarkTextSecondary
    }

    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { },
    ) {
        Column {
            // Thumbnail placeholder with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(typeColor.copy(alpha = 0.6f), DarkBgCard),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(typeIcon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
                // Duration badge
                task.durationSeconds?.let { dur ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    ) {
                        Text(
                            "${dur / 60}:${(dur % 60).toString().padStart(2, '0')}",
                            color = Color.White,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(task.title, color = DarkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(task.platformId, color = DarkTextSecondary, fontSize = 9.sp)
                    Text("${task.totalBytes / (1024 * 1024)} MB", color = DarkTextSecondary, fontSize = 9.sp)
                }
            }
        }
    }
}
