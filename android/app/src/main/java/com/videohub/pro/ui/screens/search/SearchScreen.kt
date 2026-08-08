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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.network.SearchEngine
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchEngine: SearchEngine,
) : ViewModel() {

    private val _results = MutableStateFlow<List<SearchEngine.SearchResult>>(emptyList())
    val results: StateFlow<List<SearchEngine.SearchResult>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }

        _isSearching.value = true
        viewModelScope.launch {
            // Check connectivity first
            val online = withContext(Dispatchers.IO) { searchEngine.isAvailable() }
            _isOnline.value = online

            if (!online) {
                _isSearching.value = false
                return@launch
            }

            // Perform real search
            val results = withContext(Dispatchers.IO) { searchEngine.search(query) }
            _results.value = results
            _isSearching.value = false
        }
    }

    fun clear() {
        _results.value = emptyList()
    }
}

private val TRENDING_SEARCHES = listOf(
    "موسيقى هادئة",
    "وصفات سريعة",
    "أهداف كرة القدم",
    "دروس برمجة",
    "وثائقي طبيعة",
    "مراجعات تقنية",
    "نصائح تطوير الذات",
    "أفلام قصيرة",
)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.results.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val hasResults = results.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBgPrimary),
    ) {
        // Header
        Text(
            "البحث",
            color = DarkTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        // Search bar
        Surface(
            color = DarkBgCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = DarkTextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(AmberPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text("ابحث عن فيديوهات، موسيقى...", color = DarkBorder, fontSize = 14.sp)
                        }
                        innerTextField()
                    },
                )
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "مسح",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(16.dp).clickable {
                            query = ""
                            viewModel.clear()
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Search action hint
        if (query.isNotEmpty() && !hasResults && !isSearching) {
            Surface(
                color = AmberPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { viewModel.search(query) },
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(16.dp))
                    Text("اضغط للبحث عن \"$query\"", color = AmberPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Loading indicator
        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AmberPrimary)
                    Text("جاري البحث في الإنترنت...", color = DarkTextSecondary, fontSize = 12.sp)
                }
            }
        }

        // Offline warning
        if (!isOnline && query.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("⚠️", fontSize = 32.sp)
                    Text("لا يوجد اتصال بالإنترنت", color = DarkTextSecondary, fontSize = 13.sp)
                    Text("تحقق من اتصالك وحاول مرة أخرى", color = DarkBorder, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (!hasResults && !isSearching && query.isEmpty() && isOnline) {
            // Trending searches
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(16.dp))
                    Text("عمليات بحث رائجة", color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                TRENDING_SEARCHES.forEachIndexed { idx, search ->
                    Surface(
                        color = DarkBgCard,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                query = search
                                viewModel.search(search)
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("${idx + 1}", color = AmberPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.size(24.dp))
                            Text(search, color = DarkTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = DarkBorder, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        } else if (hasResults) {
            // Real search results
            Text(
                "نتائج البحث (${results.size})",
                color = DarkTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { it.url }) { result ->
                    Surface(
                        color = DarkBgCard,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                // Open share overlay with this URL
                            },
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(result.platform.take(2).uppercase(), color = AmberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(result.title, color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Text(result.url, color = DarkTextSecondary, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
