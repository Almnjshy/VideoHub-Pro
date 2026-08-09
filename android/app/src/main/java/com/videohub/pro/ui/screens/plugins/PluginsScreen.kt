package com.videohub.pro.ui.screens.plugins

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.videohub.pro.data.database.entities.PluginEntity
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess
import com.videohub.pro.ui.theme.RedError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginsViewModel @Inject constructor(
    private val database: VideoHubDatabase,
) : ViewModel() {
    val plugins = database.pluginDao().observeAll()

    fun toggleEnabled(plugin: PluginEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.pluginDao().update(plugin.copy(enabled = !plugin.enabled))
        }
    }
}

@Composable
fun PluginsScreen(
    viewModel: PluginsViewModel = hiltViewModel(),
) {
    val plugins by viewModel.plugins.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().background(DarkBgPrimary)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("الوحدات", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${plugins.size} منصة", color = DarkTextSecondary, fontSize = 11.sp)
        }

        if (plugins.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("لا توجد وحدات", color = DarkTextSecondary, fontSize = 13.sp) }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(plugins, key = { it.id }) { plugin -> PluginCard(plugin) { viewModel.toggleEnabled(plugin) } }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun PluginCard(plugin: PluginEntity, onToggle: () -> Unit) {
    val healthColor = when (plugin.status) {
        "healthy" -> EmeraldSuccess
        "degraded" -> AmberPrimary
        "broken" -> RedError
        else -> DarkBorder
    }
    val healthLabel = when (plugin.status) {
        "healthy" -> "سليم"
        "degraded" -> "متدهور"
        "broken" -> "معطل"
        "VERIFIED" -> "مدعوم"
        "NOT_VERIFIED" -> "غير مدعوم"
        else -> plugin.status
    }
    val rate = (plugin.successRate * 100).toInt()

    Surface(color = DarkBgCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Icon
            Surface(color = Color(android.graphics.Color.parseColor(plugin.color)).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(plugin.icon, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
            // Info
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(plugin.nameAr, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("v${plugin.version} · ${plugin.name}", color = DarkTextSecondary, fontSize = 9.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${plugin.totalAttempts} محاولة", color = DarkTextSecondary, fontSize = 9.sp)
                    Text("·", color = DarkBorder, fontSize = 9.sp)
                    Text("$rate% نجاح", color = healthColor, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
            // Status badge
            Surface(color = healthColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                Text(healthLabel, color = healthColor, fontSize = 9.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(6.dp, 3.dp))
            }
        }
    }
}
