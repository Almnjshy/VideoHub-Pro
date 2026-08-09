package com.videohub.pro.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.data.database.entities.FavoriteEntity
import com.videohub.pro.resolver.ytdlp.YtDlpResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class Region(val code: String, val label: String)
data class Category(val id: String, val label: String, val icon: ImageVector, val color: Color)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val ytDlpResolver: YtDlpResolver,
    private val database: VideoHubDatabase,
) : ViewModel() {
    var trending by mutableStateOf<List<YtDlpResolver.SearchResult>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var selectedCategory by mutableStateOf("now")
    var selectedRegion by mutableStateOf("US")
    var previewItem by mutableStateOf<YtDlpResolver.SearchResult?>(null)
    var videoInfo by mutableStateOf<YtDlpResolver.VideoInfo?>(null)
    var isLoadingPreview by mutableStateOf(false)

    val favorites = database.favoriteDao().observeAll()

    private val categories = listOf(
        Category("now", "رائج", Icons.Default.Whatshot, Color(0xFFF59E0B)),
        Category("music", "موسيقى", Icons.Default.MusicNote, Color(0xFFEC4899)),
        Category("gaming", "ألعاب", Icons.Default.SportsEsports, Color(0xFF8B5CF6)),
        Category("movies", "أفلام", Icons.Default.VideoLibrary, Color(0xFF3B82F6)),
        Category("news", "أخبار", Icons.Default.Newspaper, Color(0xFF10B981)),
    )

    val regions = listOf(
        Region("US", "أمريكا"), Region("SA", "السعودية"), Region("EG", "مصر"),
        Region("AE", "الإمارات"), Region("GB", "بريطانيا"), Region("DE", "ألمانيا"),
        Region("FR", "فرنسا"), Region("JP", "اليابان"), Region("KR", "كوريا"),
        Region("IN", "الهند"), Region("BR", "البرازيل"), Region("TR", "تركيا"),
    )

    fun getCategories() = categories

    fun loadTrending(category: String = selectedCategory, region: String = selectedRegion) {
        isLoading = true; error = null; selectedCategory = category; selectedRegion = region
        viewModelScope.launch(Dispatchers.IO) {
            val result = ytDlpResolver.getTrendingByCategoryRegion(category, region)
            isLoading = false
            trending = result ?: emptyList()
            if (result == null) error = "تعذّر تحميل المحتوى"
        }
    }

    fun toggleFavorite(item: YtDlpResolver.SearchResult) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = database.favoriteDao().getByUrl(item.url)
            if (existing != null) {
                database.favoriteDao().deleteByUrl(item.url)
            } else {
                database.favoriteDao().insert(FavoriteEntity(
                    id = UUID.randomUUID().toString(), url = item.url, title = item.title,
                    uploader = item.uploader, thumbnail = item.thumbnail,
                    duration = item.duration, platform = item.platform,
                ))
            }
        }
    }

    fun isFavorite(url: String, favorites: List<FavoriteEntity>): Boolean =
        favorites.any { it.url == url }

    fun openPreview(item: YtDlpResolver.SearchResult) {
        previewItem = item; videoInfo = null; isLoadingPreview = true
        viewModelScope.launch(Dispatchers.IO) {
            videoInfo = ytDlpResolver.getVideoInfo(item.url)
            isLoadingPreview = false
        }
    }

    fun closePreview() { previewItem = null; videoInfo = null }
}

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
    onDownloadUrl: (String) -> Unit = {},
) {
    val categories = remember { viewModel.getCategories() }
    val favorites by viewModel.favorites.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        if (viewModel.trending.isEmpty() && !viewModel.isLoading) viewModel.loadTrending()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF09090B))) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Whatshot, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                Text("اكتشف", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Surface(color = Color(0xFF18181B), shape = RoundedCornerShape(10.dp), modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { viewModel.loadTrending() }.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Refresh, "تحديث", tint = Color(0xFFA1A1AA), modifier = Modifier.size(18.dp)) }
            }
        }

        // Region selector
        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(viewModel.regions) { region ->
                val selected = viewModel.selectedRegion == region.code
                Surface(color = if (selected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF18181B), shape = RoundedCornerShape(16.dp), modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { viewModel.loadTrending(viewModel.selectedCategory, region.code) }) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = if (selected) Color(0xFF3B82F6) else Color(0xFF71717A), modifier = Modifier.size(11.dp))
                        Text(region.label, color = if (selected) Color(0xFF60A5FA) else Color(0xFFA1A1AA), fontSize = 10.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Category chips
        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                val selected = viewModel.selectedCategory == category.id
                Surface(color = if (selected) category.color.copy(alpha = 0.2f) else Color(0xFF18181B), shape = RoundedCornerShape(20.dp), modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { viewModel.loadTrending(category.id) }) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(category.icon, null, tint = if (selected) category.color else Color(0xFF71717A), modifier = Modifier.size(14.dp))
                        Text(category.label, color = if (selected) category.color else Color(0xFFA1A1AA), fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            viewModel.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(36.dp)); Text("جاري التحميل...", color = Color(0xFF71717A), fontSize = 12.sp) } }
            viewModel.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("⚠️", fontSize = 36.sp); Text(viewModel.error!!, color = Color(0xFFEF4444), fontSize = 12.sp); Surface(color = Color(0xFFF59E0B).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { viewModel.loadTrending() }) { Text("إعادة المحاولة", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(20.dp, 10.dp)) } } }
            else -> LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.trending, key = { it.url }) { item ->
                    DiscoverCard(item = item, isFav = viewModel.isFavorite(item.url, favorites), onDownload = { onDownloadUrl(item.url) }, onPreview = { viewModel.openPreview(item) }, onToggleFav = { viewModel.toggleFavorite(item) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Preview Bottom Sheet
    viewModel.previewItem?.let { item ->
        PreviewSheet(item = item, videoInfo = viewModel.videoInfo, isLoading = viewModel.isLoadingPreview, favorites = favorites, onClose = { viewModel.closePreview() }, onDownload = { onDownloadUrl(item.url); viewModel.closePreview() }, onToggleFav = { viewModel.toggleFavorite(item) })
    }
}

@Composable
private fun DiscoverCard(item: YtDlpResolver.SearchResult, isFav: Boolean, onDownload: () -> Unit, onPreview: () -> Unit, onToggleFav: () -> Unit) {
    Surface(color = Color(0xFF18181B), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPreview() }) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                if (item.thumbnail != null) coil.compose.AsyncImage(item.thumbnail, item.title, Modifier.fillMaxSize()) else Box(Modifier.fillMaxSize().background(Color(0xFF27272A)), Alignment.Center) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(32.dp)) }
                if (item.duration != null && item.duration > 0) Surface(color = Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) { Text(formatDuration(item.duration), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(4.dp, 2.dp)) }
                // Favorite star
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(24.dp).clickable { onToggleFav() }) { Box(contentAlignment = Alignment.Center) { Icon(if (isFav) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "مفضلة", tint = if (isFav) Color(0xFFF59E0B) else Color.White, modifier = Modifier.size(14.dp)) } }
            }
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.uploader != null) Text(item.uploader, color = Color(0xFFA1A1AA), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.viewCount != null) Text("${formatCount(item.viewCount)} مشاهدة", color = Color(0xFF71717A), fontSize = 8.sp)
                    if (item.uploadDate != null) Text("· ${item.uploadDate!!.take(4)}", color = Color(0xFF71717A), fontSize = 8.sp)
                }
            }
        }
    }
}

@Composable
private fun PreviewSheet(item: YtDlpResolver.SearchResult, videoInfo: YtDlpResolver.VideoInfo?, isLoading: Boolean, favorites: List<FavoriteEntity>, onClose: () -> Unit, onDownload: () -> Unit, onToggleFav: () -> Unit) {
    val isFav = favorites.any { it.url == item.url }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable { onClose() }) {
        Surface(color = Color(0xFF18181B), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().clickable { }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Drag handle
                Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF52525B)))
                // Thumbnail
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))) {
                    if (item.thumbnail != null) coil.compose.AsyncImage(item.thumbnail, item.title, Modifier.fillMaxSize()) else Box(Modifier.fillMaxSize().background(Color(0xFF27272A)))
                    if (item.duration != null && item.duration > 0) Surface(color = Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) { Text(formatDuration(item.duration), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(6.dp, 2.dp)) }
                }
                // Title + Fav
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(videoInfo?.title ?: item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Surface(color = if (isFav) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF27272A), shape = RoundedCornerShape(8.dp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onToggleFav() }.size(36.dp)) { Box(contentAlignment = Alignment.Center) { Icon(if (isFav) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "مفضلة", tint = if (isFav) Color(0xFFF59E0B) else Color(0xFFA1A1AA), modifier = Modifier.size(18.dp)) } }
                }
                // Info
                if (isLoading) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(16.dp)); Text("جاري التحميل...", color = Color(0xFF71717A), fontSize = 11.sp) }
                else if (videoInfo != null) {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        videoInfo.viewCount?.let { InfoChip("${formatCount(it)} مشاهدة") }
                        videoInfo.duration?.let { InfoChip(formatDuration(it)) }
                        videoInfo.uploadDate?.let { InfoChip(it.take(4) + "/" + it.substring(4, 6) + "/" + it.substring(6, 8)) }
                    }
                    if (!videoInfo.description.isNullOrBlank()) Text(videoInfo.description!!.take(200) + "...", color = Color(0xFFA1A1AA), fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                // Actions
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    Surface(color = Color(0xFFF59E0B), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onDownload() }.padding(vertical = 12.dp), contentColor = Color.Black) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Download, null, Modifier.size(18.dp)); Text("تنزيل", fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                    Surface(color = Color(0xFF27272A), shape = RoundedCornerShape(12.dp), modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { }.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color(0xFFA1A1AA), modifier = Modifier.size(18.dp)) } }
                    Surface(color = Color(0xFF27272A), shape = RoundedCornerShape(12.dp), modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClose() }.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFFA1A1AA), modifier = Modifier.size(18.dp)) } }
                }
            }
        }
    }
}

@Composable private fun InfoChip(text: String) { Surface(color = Color(0xFF27272A), shape = RoundedCornerShape(6.dp)) { Text(text, color = Color(0xFFA1A1AA), fontSize = 10.sp, modifier = Modifier.padding(6.dp, 3.dp)) } }
private fun formatCount(c: Long): String { return if (c < 1000) c.toString() else if (c < 1_000_000) "${c / 1000}K" else if (c < 1_000_000_000) "${c / 1_000_000}M" else "${c / 1_000_000_000}B" }
private fun formatDuration(s: Int): String { val h = s / 3600; val m = (s % 3600) / 60; val ss = s % 60; return if (h > 0) "$h:${String.format("%02d", m)}:${String.format("%02d", ss)}" else "$m:${String.format("%02d", ss)}" }
