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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val database: VideoHubDatabase,
) : ViewModel() {
    val notifications = database.notificationDao().observeAll()

    fun markAsRead(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.notificationDao().markRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            database.notificationDao().markAllRead()
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            database.notificationDao().clearAll()
        }
    }
}

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())
    val unreadCount = notifications.count { !it.read }
    val df = remember { SimpleDateFormat("h:mm a · dd/MM", Locale("ar")) }

    Column(Modifier.fillMaxSize().background(DarkBgPrimary)) {
        // Header
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("الإشعارات", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (notifications.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (unreadCount > 0) {
                        Surface(color = AmberPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { viewModel.markAllAsRead() }) {
                            Text("تعليم الكل كمقروء", color = AmberPrimary, fontSize = 10.sp, modifier = Modifier.padding(8.dp, 4.dp))
                        }
                    }
                    Surface(color = RedError.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { viewModel.clearAll() }) {
                        Icon(Icons.Default.Delete, contentDescription = "مسح الكل", tint = RedError, modifier = Modifier.size(16.dp).padding(4.dp))
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔔", fontSize = 32.sp)
                    Text("لا توجد إشعارات", color = DarkTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationCard(notif, df) { viewModel.markAsRead(notif.id) }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun NotificationCard(notif: NotificationEntity, df: SimpleDateFormat, onClick: () -> Unit) {
    val (icon, color) = when (notif.type) {
        "task_completed" -> Icons.Default.CheckCircle to EmeraldSuccess
        "task_failed" -> Icons.Default.Error to RedError
        "storage_warning" -> Icons.Default.Warning to AmberPrimary
        "task_started" -> Icons.Default.Info to Color(0xFF3B82F6)
        "task_retry" -> Icons.Default.Warning to Color(0xFFF97316)
        else -> Icons.Default.Info to DarkBorder
    }
    val bgAlpha = if (notif.read) 0f else 0.08f

    Surface(
        color = if (!notif.read) color.copy(alpha = bgAlpha) else DarkBgCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(notif.title, color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(notif.message, color = DarkTextSecondary, fontSize = 10.sp, maxLines = 2)
                Text(df.format(Date(notif.timestamp)), color = DarkBorder, fontSize = 8.sp)
            }
            if (!notif.read) { Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color)) }
        }
    }
}

@Composable
private fun <T> remember(calculation: () -> T): T = androidx.compose.runtime.remember(calculation = calculation)
