package com.videohub.pro.ui.screens.discover

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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary

data class DiscoverItem(
    val title: String,
    val author: String,
    val platform: String,
    val views: String,
    val duration: String,
    val category: String,
    val gradient: List<Color>,
)

private val SAMPLE_ITEMS = listOf(
    DiscoverItem("أفضل لحظات كرة القدم", "Sports HD", "youtube", "2.4M", "8:24", "trending", listOf(Color(0xFF10B981), Color(0xFF3B82F6))),
    DiscoverItem("مراجعة أحدث هاتف رائد", "Tech Review", "youtube", "1.1M", "12:05", "tech", listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))),
    DiscoverItem("وصفة الكيك الأسهل", "Cooking Pro", "tiktok", "5.7M", "0:45", "trending", listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
    DiscoverItem("جولة في أجمل مدن أوروبا", "Travel Diaries", "instagram", "890K", "2:15", "trending", listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
    DiscoverItem("شرح React 19 من الصفر", "Code Academy", "youtube", "340K", "45:18", "education", listOf(Color(0xFF3B82F6), Color(0xFF6366F1))),
    DiscoverItem("مقطع موسيقي هادئ", "Relax Music", "soundcloud", "1.8M", "30:00", "music", listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))),
    DiscoverItem("أفضل أهداف هذا الموسم", "Football Zone", "facebook", "3.2M", "5:42", "trending", listOf(Color(0xFF22C55E), Color(0xFF10B981))),
    DiscoverItem("أخبار التقنية لهذا الأسبوع", "Tech News", "x", "670K", "3:20", "tech", listOf(Color(0xFF64748B), Color(0xFF475569))),
)

private data class Category(val id: String, val label: String, val icon: ImageVector)

private val CATEGORIES = listOf(
    Category("all", "الكل", Icons.Default.Explore),
    Category("trending", "رائج", Icons.Default.Whatshot),
    Category("music", "موسيقى", Icons.Default.MusicNote),
    Category("gaming", "ألعاب", Icons.Default.SportsEsports),
    Category("education", "تعليم", Icons.Default.School),
    Category("tech", "تقنية", Icons.Default.Explore),
)

@Composable
fun DiscoverScreen() {
    var selectedCategory by remember { mutableStateOf("all") }
    val filtered = if (selectedCategory == "all") SAMPLE_ITEMS
        else SAMPLE_ITEMS.filter { it.category == selectedCategory }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBgPrimary),
    ) {
        // Header
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("اكتشف", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("محتوى رائج عبر كل المنصات", color = DarkTextSecondary, fontSize = 12.sp)
        }

        // Categories
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CATEGORIES.forEach { category ->
                val selected = selectedCategory == category.id
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
                        Icon(category.icon, contentDescription = null, tint = if (selected) AmberPrimary else DarkTextSecondary, modifier = Modifier.size(14.dp))
                        Text(category.label, color = if (selected) AmberPrimary else DarkTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Featured item (first)
        filtered.firstOrNull()?.let { featured ->
            Surface(
                color = DarkBgCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { },
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Brush.linearGradient(featured.gradient)),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Surface(color = AmberPrimary, shape = RoundedCornerShape(4.dp)) {
                                Text("مميز", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text(featured.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${featured.author} · ${featured.views} · ${featured.duration}", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Grid of remaining items
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered.drop(1)) { item ->
                DiscoverItemRow(item)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DiscoverItemRow(item: DiscoverItem) {
    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { },
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(item.gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.platform, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(item.author, color = DarkTextSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.views, color = DarkBorder, fontSize = 9.sp)
                    Text("·", color = DarkBorder, fontSize = 9.sp)
                    Text(item.duration, color = DarkBorder, fontSize = 9.sp)
                }
            }
        }
    }
}
