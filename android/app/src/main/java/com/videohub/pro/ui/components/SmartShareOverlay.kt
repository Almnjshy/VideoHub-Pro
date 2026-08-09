package com.videohub.pro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata
import com.videohub.pro.domain.models.MediaType
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBgSecondary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess

/**
 * Smart Share Overlay — Modal Bottom Sheet
 *
 * Behavior:
 *  - Slides up from bottom (40-60% screen height)
 *  - Original app remains visible in background (dimmed)
 *  - No full app launch — just the bottom sheet
 */
@Composable
fun SmartShareOverlay(
    url: String,
    onDismiss: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Always fetch fresh metadata when a new URL is received
    LaunchedEffect(url) {
        viewModel.resolveUrl(url)
    }

    // Dimmed background + bottom sheet
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                ShareBottomSheet(
                    url = url,
                    state = state,
                    onDismiss = onDismiss,
                    onStartDownload = { format, priority ->
                        viewModel.startDownload(url, format, priority)
                        onDismiss()
                    },
                    modifier = Modifier.clickable(enabled = false) {}, // prevent click-through
                )
            }
        }
    }
}

@Composable
private fun ShareBottomSheet(
    url: String,
    state: ShareState,
    onDismiss: () -> Unit,
    onStartDownload: (MediaFormat, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkBgSecondary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkBorder)
                    .align(Alignment.CenterHorizontally),
            )

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("مشاركة ذكية", color = DarkTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("VideoHub Pro", color = DarkTextSecondary, fontSize = 10.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBorder.copy(alpha = 0.5f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = DarkTextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            // URL
            Surface(
                color = DarkBgPrimary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    url,
                    color = DarkTextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Plugin detected
            state.detectedPlugin?.let { plugin ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBorder.copy(alpha = 0.5f))
                        .padding(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(parseColor(plugin.color)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(plugin.icon, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(plugin.nameAr, color = DarkTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("الوحدة v${plugin.version}", color = DarkTextSecondary, fontSize = 10.sp)
                    }
                    Surface(color = EmeraldSuccess.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                        Text("متعرف عليها", color = EmeraldSuccess, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            // Loading
            if (state.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .align(Alignment.CenterHorizontally),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AmberPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("جاري تحليل الرابط واستخراج البيانات...", color = DarkTextSecondary, fontSize = 13.sp)
                }
            }

            // Error
            state.error?.let { error ->
                Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(error, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                }
            }

            // Metadata + Formats (only show if formats exist)
            if (state.formats.isNotEmpty() && state.metadata != null) {
                MetadataAndFormats(
                    metadata = state.metadata!!,
                    formats = state.formats,
                    onStartDownload = onStartDownload,
                )
            } else if (!state.isLoading && state.error == null) {
                // No formats and no error — show "unable to resolve" message
                Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("تعذّر استخراج رابط تنزيل", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("قد تكون المنصة غير مدعومة أو تتطلب معالجة خاصة", color = Color.Red.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataAndFormats(
    metadata: MediaMetadata,
    formats: List<MediaFormat>,
    onStartDownload: (MediaFormat, Int) -> Unit,
) {
    var selectedFormatId by remember { mutableStateOf(formats.firstOrNull()?.id ?: "") }
    var priority by remember { mutableStateOf(1) }

    val videoFormats = formats.filter { it.mediaType == MediaType.VIDEO }
    val audioFormats = formats.filter { it.mediaType == MediaType.AUDIO }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Media info
        Row {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBorder),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(metadata.title, color = DarkTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                metadata.author?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, color = DarkTextSecondary, fontSize = 11.sp)
                }
                metadata.durationSeconds?.let { dur ->
                    Spacer(Modifier.height(4.dp))
                    Surface(color = DarkBorder, shape = RoundedCornerShape(4.dp)) {
                        Text("${dur / 60}:${(dur % 60).toString().padStart(2, '0')}", color = DarkTextSecondary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }

        // Video section
        if (videoFormats.isNotEmpty()) {
            FormatSection(title = "تنزيل كفيديو", subtitle = "${videoFormats.size} جودات", icon = Icons.Default.Movie, accentColor = AmberPrimary, formats = videoFormats, selectedFormatId = selectedFormatId, onSelect = { selectedFormatId = it })
        }

        // Audio section
        if (audioFormats.isNotEmpty()) {
            FormatSection(title = "تنزيل كصوت", subtitle = "${audioFormats.size} صيغ", icon = Icons.Default.MusicNote, accentColor = EmeraldSuccess, formats = audioFormats, selectedFormatId = selectedFormatId, onSelect = { selectedFormatId = it })
        }

        // Priority
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("عالية" to 0, "عادية" to 1, "منخفضة" to 2).forEach { (label, value) ->
                PriorityChip(label = label, selected = priority == value, onClick = { priority = value }, modifier = Modifier.weight(1f))
            }
        }

        // Start button
        Button(
            onClick = {
                formats.find { it.id == selectedFormatId }?.let { onStartDownload(it, priority) }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("بدء التنزيل", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun FormatSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    formats: List<MediaFormat>,
    selectedFormatId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(accentColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, color = DarkTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(subtitle, color = DarkTextSecondary, fontSize = 9.sp)
            }
        }

        formats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { fmt ->
                    FormatChip(format = fmt, selected = fmt.id == selectedFormatId, accentColor = accentColor, onClick = { onSelect(fmt.id) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FormatChip(format: MediaFormat, selected: Boolean, accentColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) accentColor.copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.5f)
    val border = if (selected) accentColor.copy(alpha = 0.5f) else DarkBorder
    val textColor = if (selected) accentColor else DarkTextPrimary

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, border),
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(format.label, color = textColor, fontWeight = FontWeight.Medium, fontSize = 11.sp)
            Text(formatSize(format.sizeBytes), color = textColor.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun PriorityChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) AmberPrimary.copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.5f)
    val border = if (selected) AmberPrimary.copy(alpha = 0.5f) else DarkBorder
    val textColor = if (selected) AmberPrimary else DarkTextPrimary

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, border),
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
    ) {
        Text(label, color = textColor, fontWeight = FontWeight.Medium, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    if (bytes < 1024 * 1024) return "${bytes / 1024}KB"
    if (bytes < 1024 * 1024 * 1024) return "${bytes / (1024 * 1024)}MB"
    return "%.2fGB".format(bytes / (1024.0 * 1024 * 1024))
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        DarkBorder
    }
}
