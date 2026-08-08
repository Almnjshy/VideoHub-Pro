package com.videohub.pro.ui.screens.notifications

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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.data.database.entities.NotificationEntity
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
class NotificationsViewModel @Inject constructor(
    database: VideoHubDatabase,
) : ViewModel() {
    val notifications = database.notificationDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBgPrimary),
    ) {
        Text(
            "الإشعارات",
            color = DarkTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔕", fontSize = 32.sp)
                    Text("لا توجد إشعارات", color = DarkTextSecondary, fontSize = 13.sp)
                    Text("ستظهر هنا عند اكتمال أو فشل التنزيلات", color = DarkBorder, fontSize = 10.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationCard(notif)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun NotificationCard(notif: NotificationEntity) {
    val iconAndColor: Pair<ImageVector, Color> = when (notif.type) {
        "task_completed" -> Icons.Default.CheckCircle to EmeraldSuccess
        "task_failed" -> Icons.Default.Warning to RedError
        "task_started" -> Icons.Default.Download to AmberPrimary
        "task_retry" -> Icons.Default.Autorenew to Color(0xFFFB923C)
        "plugin_broken" -> Icons.Default.Extension to RedError
        "plugin_degraded" -> Icons.Default.Extension to AmberPrimary
        "storage_warning" -> Icons.Default.Storage to AmberPrimary
        else -> Icons.Default.CheckCircle to DarkTextSecondary
    }
    val icon = iconAndColor.first
    val color = iconAndColor.second

    Surface(
        color = if (!notif.read) DarkBgCard else DarkBgCard.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(notif.title, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(notif.message, color = DarkTextSecondary, fontSize = 11.sp, maxLines = 2)
                Text(formatTime(notif.timestamp), color = DarkBorder, fontSize = 9.sp)
            }
            if (!notif.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AmberPrimary),
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "الآن"
        diff < 3_600_000 -> "منذ ${diff / 60_000} دقيقة"
        diff < 86_400_000 -> "منذ ${diff / 3_600_000} ساعة"
        else -> "منذ ${diff / 86_400_000} يوم"
    }
}
