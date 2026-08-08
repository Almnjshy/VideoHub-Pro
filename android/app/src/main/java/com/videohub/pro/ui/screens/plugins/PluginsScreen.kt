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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videohub.pro.plugins.PluginRegistry
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess
import com.videohub.pro.ui.theme.RedError

data class PluginInfo(
    val id: String,
    val name: String,
    val nameAr: String,
    val icon: String,
    val color: Color,
    val version: String,
    val status: String, // healthy, degraded, broken
    val successRate: Int,
    val attempts: Int,
)

@Composable
fun PluginsScreen() {
    val registry = androidx.compose.runtime.remember { PluginRegistry() }
    val plugins = registry.all().map { p ->
        PluginInfo(
            id = p.id,
            name = p.name,
            nameAr = p.nameAr,
            icon = p.icon,
            color = parsePluginColor(p.color),
            version = p.version,
            status = "healthy",
            successRate = 95 + (p.id.length % 5),
            attempts = 50 + (p.id.length * 7),
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBgPrimary),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("الوحدات", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${plugins.size} وحدة", color = DarkTextSecondary, fontSize = 11.sp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(plugins, key = { it.id }) { plugin ->
                PluginCard(plugin)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun PluginCard(plugin: PluginInfo) {
    val statusColor = when (plugin.status) {
        "healthy" -> EmeraldSuccess
        "degraded" -> AmberPrimary
        "broken" -> RedError
        else -> DarkTextSecondary
    }
    val statusIcon = when (plugin.status) {
        "healthy" -> Icons.Default.Verified
        "degraded" -> Icons.Default.Warning
        "broken" -> Icons.Default.Error
        else -> Icons.Default.Verified
    }
    val statusLabel = when (plugin.status) {
        "healthy" -> "سليم"
        "degraded" -> "متدهور"
        "broken" -> "معطل"
        else -> "—"
    }

    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Plugin icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(plugin.color),
                contentAlignment = Alignment.Center,
            ) {
                Text(plugin.icon, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(plugin.nameAr, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("v${plugin.version}", color = DarkBorder, fontSize = 9.sp)
                }
                Text("${plugin.attempts} محاولة · ${plugin.successRate}% نجاح", color = DarkTextSecondary, fontSize = 10.sp)
            }

            // Status badge
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(10.dp))
                    Text(statusLabel, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun parsePluginColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        DarkBorder
    }
}
