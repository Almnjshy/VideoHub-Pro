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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.data.database.entities.FavoriteEntity
import com.videohub.pro.ui.theme.AmberPrimary
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MediaFile(
    val file: File,
    val title: String,
    val isVideo: Boolean,
    val size: Long,
    val lastModified: Long,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val database: VideoHubDatabase,
) : ViewModel() {
    val favorites = database.favoriteDao().observeAll()

    fun deleteCompletedTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) { database.taskDao().delete(taskId) }
    }

    fun deleteFavorite(url: String) {
        viewModelScope.launch(Dispatchers.IO) { database.favoriteDao().deleteByUrl(url) }
    }

    fun deleteFile(file: java.io.File, onDeleted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            file.delete()
            onDeleted()
        }
    }
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onPlayFile: (filePath: String, title: String, isVideo: Boolean) -> Unit = { _, _, _ -> },
    onDownloadUrl: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var mediaFiles by remember { mutableStateOf<List<MediaFile>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }
    var deleteFileTarget by remember { mutableStateOf<MediaFile?>(null) }
    var deleteFavTarget by remember { mutableStateOf<FavoriteEntity?>(null) }
    val favorites by viewModel.favorites.collectAsState(initial = emptyList())

    // Load real files
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            withContext(Dispatchers.IO) {
                val dir = File(context.getExternalFilesDir(null), "VideoHub Pro")
                val files = mutableListOf<MediaFile>()
                if (dir.exists()) {
                    dir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val ext = file.extension.lowercase()
                            val isVideo = ext in listOf("mp4", "webm", "mkv", "avi", "mov")
                            val isAudio = ext in listOf("mp3", "m4a", "aac", "ogg", "wav")
                            if (isVideo || isAudio) {
                                files.add(MediaFile(file, file.nameWithoutExtension, isVideo, file.length(), file.lastModified()))
                            }
                        }
                    }
                }
                files.sortByDescending { it.lastModified }
                mediaFiles = files
            }
        }
    }

    Column(Modifier.fillMaxSize().background(DarkBgPrimary)) {
        Text("المكتبة", color = Color(0xFFFAFAFA), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

        // Tabs: Files | Favorites
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkBgCard,
            contentColor = AmberPrimary,
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("الملفات (${mediaFiles.size})", fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("المفضلة (${favorites.size})", fontSize = 12.sp) })
        }

        when (selectedTab) {
            0 -> {
                // Files tab
                val totalSize = mediaFiles.sumOf { it.size }
                if (mediaFiles.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📂", fontSize = 32.sp)
                            Text("لا توجد ملفات", color = DarkTextSecondary, fontSize = 13.sp)
                            Text("نزّل بعض الفيديوهات لتظهر هنا", color = DarkBorder, fontSize = 10.sp)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                        Text("إجمالي: ", color = DarkTextSecondary, fontSize = 11.sp)
                        Text(StorageHelper.formatBytes(totalSize), color = DarkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(mediaFiles, key = { it.file.absolutePath }) { mf ->
                            MediaFileCard(mf, onPlay = { onPlayFile(mf.file.absolutePath, mf.title, mf.isVideo) }, onShare = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = if (mf.isVideo) "video/*" else "audio/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mf.file))
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "مشاركة"))
                            }, onDelete = { deleteFileTarget = mf })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
            1 -> {
                // Favorites tab
                if (favorites.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⭐", fontSize = 32.sp)
                            Text("لا توجد مفضلة", color = DarkTextSecondary, fontSize = 13.sp)
                            Text("احفظ الفيديوهات من اكتشف أو بحث", color = DarkBorder, fontSize = 10.sp)
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(favorites, key = { it.id }) { fav ->
                            FavoriteCard(fav, onDownload = { onDownloadUrl(fav.url) }, onDelete = { deleteFavTarget = fav })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Delete dialogs
    deleteFileTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteFileTarget = null },
            title = { Text("حذف الملف", color = DarkTextPrimary) },
            text = { Text("هل تريد حذف \"${file.title}\"؟", color = DarkTextSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.deleteFile(file.file) { }; mediaFiles = mediaFiles.filterNot { it == file }; deleteFileTarget = null }) { Text("حذف", color = Color(0xFFEF4444)) } },
            dismissButton = { TextButton(onClick = { deleteFileTarget = null }) { Text("إلغاء", color = DarkTextSecondary) } },
        )
    }
    deleteFavTarget?.let { fav ->
        AlertDialog(
            onDismissRequest = { deleteFavTarget = null },
            title = { Text("حذف من المفضلة", color = DarkTextPrimary) },
            text = { Text("إزالة \"${fav.title}\" من المفضلة؟", color = DarkTextSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.deleteFavorite(fav.url); deleteFavTarget = null }) { Text("حذف", color = Color(0xFFEF4444)) } },
            dismissButton = { TextButton(onClick = { deleteFavTarget = null }) { Text("إلغاء", color = DarkTextSecondary) } },
        )
    }
}

@Composable
private fun MediaFileCard(mf: MediaFile, onPlay: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    val df = remember { SimpleDateFormat("yyyy/MM/dd", Locale("ar")) }
    Surface(color = DarkBgCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = if (mf.isVideo) EmeraldSuccess.copy(alpha = 0.15f) else AmberPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(if (mf.isVideo) Icons.Default.VideoLibrary else Icons.Default.MusicNote, contentDescription = null, tint = if (mf.isVideo) EmeraldSuccess else AmberPrimary, modifier = Modifier.size(22.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(mf.title, color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (mf.isVideo) "فيديو" else "صوت", color = DarkTextSecondary, fontSize = 9.sp)
                    Text("·", color = DarkBorder, fontSize = 9.sp)
                    Text(StorageHelper.formatBytesShort(mf.size), color = DarkTextSecondary, fontSize = 9.sp)
                    Text("·", color = DarkBorder, fontSize = 9.sp)
                    Text(df.format(Date(mf.lastModified)), color = DarkTextSecondary, fontSize = 9.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionIcon(EmeraldSuccess, Icons.Default.PlayArrow, "تشغيل", onPlay)
                ActionIcon(Color(0xFF3B82F6), Icons.Default.Share, "مشاركة", onShare)
                ActionIcon(Color(0xFFEF4444), Icons.Default.Delete, "حذف", onDelete)
            }
        }
    }
}

@Composable
private fun FavoriteCard(fav: FavoriteEntity, onDownload: () -> Unit, onDelete: () -> Unit) {
    Surface(color = DarkBgCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (fav.thumbnail != null) {
                coil.compose.AsyncImage(fav.thumbnail, fav.title, Modifier.size(64.dp, 48.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Surface(color = DarkBorder, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(64.dp, 48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp)) }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(fav.title, color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                if (fav.uploader != null) Text(fav.uploader, color = DarkTextSecondary, fontSize = 10.sp, maxLines = 1)
                if (fav.duration != null) Text(formatDuration(fav.duration!!), color = DarkTextSecondary, fontSize = 9.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionIcon(AmberPrimary, Icons.Default.Download, "تنزيل", onDownload)
                ActionIcon(Color(0xFFEF4444), Icons.Default.Delete, "حذف", onDelete)
            }
        }
    }
}

@Composable
private fun ActionIcon(color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onClick() }) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp)) }
    }
}

private fun formatDuration(s: Int): String { val m = s / 60; val ss = s % 60; return "$m:${String.format("%02d", ss)}" }
