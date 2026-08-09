package com.videohub.pro.ui.screens.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.data.database.entities.FavoriteEntity
import com.videohub.pro.data.database.entities.SearchHistoryEntity
import com.videohub.pro.resolver.ytdlp.YtDlpResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SearchPlatform(val id: String, val label: String, val color: Color)
data class FilterOption(val id: String, val label: String)
data class SortOption(val id: String, val label: String)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val ytDlpResolver: YtDlpResolver,
    private val database: VideoHubDatabase,
) : ViewModel() {
    var results by mutableStateOf<List<YtDlpResolver.SearchResult>>(emptyList())
    var isLoading by mutableStateOf(false)
    var hasSearched by mutableStateOf(false)
    var selectedPlatform by mutableStateOf("youtube")
    var sortBy by mutableStateOf("relevance")
    var durationFilter by mutableStateOf("any")
    var timeFilter by mutableStateOf("any")
    var suggestions by mutableStateOf<List<String>>(emptyList())
    var showSuggestions by mutableStateOf(false)
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    var lastQuery by mutableStateOf("")

    val searchHistory = database.searchHistoryDao().observeRecent()
    val favorites = database.favoriteDao().observeAll()

    val platforms = listOf(
        SearchPlatform("youtube", "YouTube", Color(0xFFFF0000)),
        SearchPlatform("youtube_music", "Music", Color(0xFFEC4899)),
        SearchPlatform("soundcloud", "SoundCloud", Color(0xFFFF5500)),
    )

    val sortOptions = listOf(
        SortOption("relevance", "الصلة"), SortOption("views", "المشاهدات"),
        SortOption("date", "التاريخ"), SortOption("rating", "التقييم"),
    )

    val durationOptions = listOf(
        FilterOption("any", "الكل"), FilterOption("short", "< 4د"),
        FilterOption("medium", "4-20د"), FilterOption("long", "> 20د"),
    )

    val timeOptions = listOf(
        FilterOption("any", "الكل"), FilterOption("day", "اليوم"),
        FilterOption("week", "الأسبوع"), FilterOption("month", "الشهر"), FilterOption("year", "السنة"),
    )

    fun search(query: String) {
        if (query.isBlank()) { results = emptyList(); hasSearched = false; showSuggestions = false; return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            isLoading = true; hasSearched = true; lastQuery = query; showSuggestions = false
            saveToHistory(query)
            val result = ytDlpResolver.searchWithFilters(query, selectedPlatform, 15, sortBy, durationFilter, timeFilter)
            isLoading = false
            results = result ?: emptyList()
        }
    }

    fun updateSuggestions(query: String) {
        if (query.length < 2) { suggestions = emptyList(); return }
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch(Dispatchers.IO) {
            delay(150)
            suggestions = ytDlpResolver.getSearchSuggestions(query)
            showSuggestions = suggestions.isNotEmpty()
        }
    }

    fun setPlatform(p: String) { selectedPlatform = p; if (lastQuery.isNotEmpty()) search(lastQuery) }
    fun setSort(s: String) { sortBy = s; if (lastQuery.isNotEmpty()) search(lastQuery) }
    fun setDuration(d: String) { durationFilter = d; if (lastQuery.isNotEmpty()) search(lastQuery) }
    fun setTime(t: String) { timeFilter = t; if (lastQuery.isNotEmpty()) search(lastQuery) }

    private suspend fun saveToHistory(query: String) {
        database.searchHistoryDao().insert(SearchHistoryEntity(id = UUID.randomUUID().toString(), query = query))
    }

    fun clearHistory() { viewModelScope.launch(Dispatchers.IO) { database.searchHistoryDao().clearAll() } }
    fun deleteHistoryItem(id: String) { viewModelScope.launch(Dispatchers.IO) { database.searchHistoryDao().deleteById(id) } }

    fun toggleFavorite(item: YtDlpResolver.SearchResult) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = database.favoriteDao().getByUrl(item.url)
            if (existing != null) database.favoriteDao().deleteByUrl(item.url)
            else database.favoriteDao().insert(FavoriteEntity(id = UUID.randomUUID().toString(), url = item.url, title = item.title, uploader = item.uploader, thumbnail = item.thumbnail, duration = item.duration, platform = item.platform))
        }
    }

    fun isFavorite(url: String, favs: List<FavoriteEntity>): Boolean = favs.any { it.url == url }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onDownloadUrl: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val history by viewModel.searchHistory.collectAsState(initial = emptyList())
    val favorites by viewModel.favorites.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().background(Color(0xFF09090B))) {
        // Search bar
        OutlinedTextField(
            value = query, onValueChange = { query = it; viewModel.updateSuggestions(it); if (it.isBlank()) viewModel.search("") },
            placeholder = { Text("ابحث عن فيديو...", color = Color(0xFF71717A), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(20.dp)) },
            trailingIcon = { if (query.isNotEmpty()) Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF71717A), modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).clickable { query = ""; viewModel.search("") }) },
            singleLine = true, shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFF59E0B).copy(alpha = 0.5f), unfocusedBorderColor = Color(0xFF27272A), focusedContainerColor = Color(0xFF18181B), unfocusedContainerColor = Color(0xFF18181B)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        // Suggestions dropdown
        if (viewModel.showSuggestions && viewModel.suggestions.isNotEmpty()) {
            Surface(color = Color(0xFF18181B), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column { viewModel.suggestions.take(5).forEach { suggestion ->
                    Row(Modifier.fillMaxWidth().clickable { query = suggestion; viewModel.search(suggestion) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(14.dp))
                        Text(suggestion, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } }
            }
        }

        // Platform + filters
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(viewModel.platforms) { p ->
                val sel = viewModel.selectedPlatform == p.id
                Surface(color = if (sel) p.color.copy(alpha = 0.2f) else Color(0xFF18181B), shape = RoundedCornerShape(16.dp), modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { viewModel.setPlatform(p.id) }) { Text(p.label, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (sel) p.color else Color(0xFFA1A1AA), fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }

        // Sort + Duration + Time filters
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Sort
            items(viewModel.sortOptions) { opt ->
                val sel = viewModel.sortBy == opt.id
                Surface(color = if (sel) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF18181B), shape = RoundedCornerShape(14.dp), modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { viewModel.setSort(opt.id) }) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (sel) Icon(Icons.Default.Sort, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(11.dp))
                        Text(opt.label, fontSize = 10.sp, color = if (sel) Color(0xFFF59E0B) else Color(0xFF71717A), fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal)
                    }
                }
            }
            // Duration
            items(viewModel.durationOptions) { opt ->
                val sel = viewModel.durationFilter == opt.id
                Surface(color = if (sel) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color(0xFF18181B), shape = RoundedCornerShape(14.dp), modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { viewModel.setDuration(opt.id) }) { Text(opt.label, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = if (sel) Color(0xFF60A5FA) else Color(0xFF71717A), fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal) }
            }
            // Time
            items(viewModel.timeOptions) { opt ->
                val sel = viewModel.timeFilter == opt.id
                Surface(color = if (sel) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF18181B), shape = RoundedCornerShape(14.dp), modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { viewModel.setTime(opt.id) }) { Text(opt.label, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = if (sel) Color(0xFF34D399) else Color(0xFF71717A), fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal) }
            }
        }

        Spacer(Modifier.height(4.dp))

        when {
            viewModel.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(36.dp)) }
            !viewModel.hasSearched && history.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(16.dp)); Text("سجل البحث", color = Color(0xFFA1A1AA), fontSize = 12.sp, fontWeight = FontWeight.Medium) }; Surface(color = Color(0xFF27272A), shape = RoundedCornerShape(8.dp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { viewModel.clearHistory() }) { Row(Modifier.padding(8.dp, 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(12.dp)); Text("مسح", color = Color(0xFF71717A), fontSize = 10.sp) } } } }
                items(history, key = { it.id }) { item ->
                    Row(Modifier.fillMaxWidth().clickable { query = item.query; viewModel.search(item.query) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF52525B), modifier = Modifier.size(14.dp))
                        Text(item.query, color = Color(0xFFA1A1AA), fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF52525B), modifier = Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).clickable { viewModel.deleteHistoryItem(item.id) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
            viewModel.hasSearched && viewModel.results.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("🔍", fontSize = 40.sp); Text("لا توجد نتائج", color = Color(0xFF71717A), fontSize = 14.sp); Text("جرب كلمات أخرى أو غيّر الفلاتر", color = Color(0xFF52525B), fontSize = 10.sp) } }
            !viewModel.hasSearched -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("🔍", fontSize = 48.sp); Text("ابحث عن أي فيديو", color = Color(0xFF71717A), fontSize = 14.sp); Text("YouTube · Music · SoundCloud", color = Color(0xFF52525B), fontSize = 10.sp) } }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.results, key = { it.url }) { item ->
                    SearchResultCard(item = item, query = viewModel.lastQuery, isFav = viewModel.isFavorite(item.url, favorites), onClick = { onDownloadUrl(item.url) }, onToggleFav = { viewModel.toggleFavorite(item) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SearchResultCard(item: YtDlpResolver.SearchResult, query: String, isFav: Boolean, onClick: () -> Unit, onToggleFav: () -> Unit) {
    Surface(color = Color(0xFF18181B), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Thumbnail
            Box(Modifier.size(100.dp, 56.dp).clip(RoundedCornerShape(8.dp))) {
                if (item.thumbnail != null) coil.compose.AsyncImage(item.thumbnail, item.title, Modifier.fillMaxSize()) else Box(Modifier.fillMaxSize().background(Color(0xFF27272A)), Alignment.Center) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(20.dp)) }
                if (item.duration != null && item.duration > 0) Surface(color = Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(3.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)) { Text(formatDuration(item.duration), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(3.dp, 1.dp)) }
                Surface(color = Color(0xFFF59E0B).copy(alpha = 0.9f), shape = RoundedCornerShape(4.dp), modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(18.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Download, contentDescription = "تنزيل", tint = Color.Black, modifier = Modifier.size(12.dp)) } }
            }
            // Info
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // #16: Highlight search query in title
                Text(highlightQuery(item.title, query), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.uploader != null) Text(item.uploader, color = Color(0xFFA1A1AA), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.viewCount != null) Text("${formatCount(item.viewCount)} مشاهدة", color = Color(0xFF71717A), fontSize = 9.sp)
                    if (item.uploadDate != null) Text("· ${item.uploadDate!!.take(4)}", color = Color(0xFF71717A), fontSize = 9.sp)
                }
            }
            // #4: Favorite star
            Surface(
                color = if (isFav) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleFav() }
                    .size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (isFav) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "مفضلة", tint = if (isFav) Color(0xFFF59E0B) else Color(0xFF52525B), modifier = Modifier.size(16.dp), )
                }
            }
        }
    }
}

// #16: Highlight the search query in results
private fun highlightQuery(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var lastIdx = 0
        var idx = lowerText.indexOf(lowerQuery)
        while (idx >= 0) {
            append(text.substring(lastIdx, idx))
            withStyle(SpanStyle(color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)) {
                append(text.substring(idx, idx + query.length))
            }
            lastIdx = idx + query.length
            idx = lowerText.indexOf(lowerQuery, lastIdx)
        }
        append(text.substring(lastIdx))
    }
}

private fun formatCount(c: Long): String { return if (c < 1000) c.toString() else if (c < 1_000_000) "${c / 1000}K" else if (c < 1_000_000_000) "${c / 1_000_000}M" else "${c / 1_000_000_000}B" }
private fun formatDuration(s: Int): String { val h = s / 3600; val m = (s % 3600) / 60; val ss = s % 60; return if (h > 0) "$h:${String.format("%02d", m)}:${String.format("%02d", ss)}" else "$m:${String.format("%02d", ss)}" }
