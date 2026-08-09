package com.videohub.pro.ui.screens.workspace

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videohub.pro.data.database.entities.TaskEntity
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgCard
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import com.videohub.pro.ui.theme.EmeraldSuccess
import com.videohub.pro.utils.StorageHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Media Workspace — أدوات متقدمة للملفات المنزّلة.
 * مطابقة لنسخة الويب: تشغيل، مشاركة، استخراج صوت، قص، ضغط، معلومات.
 */
@Composable
fun MediaWorkspaceScreen(
    task: TaskEntity,
    onPlay: (String, String, Boolean) -> Unit,
    onShare: (String) -> Unit,
    onClose: () -> Unit,
) {
    val file = remember(task.outputPath) {
        task.outputPath?.let { File(it) }
    }
    val fileExists = file?.exists() == true
    val fileSize = if (fileExists) file!!.length() else task.totalBytes
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd h:mm a", Locale("ar")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = AmberPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Column {
                    Text("مساحة عمل الوسائط", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("VideoHub Pro", color = DarkTextSecondary, fontSize = 10.sp)
                }
            }
            Surface(
                color = DarkBgCard,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClose() }.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = DarkTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // File info card
        Surface(
            color = DarkBgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(task.title, color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoChip("المنصة", task.platformId.uppercase(), AmberPrimary)
                    InfoChip("الجودة", task.formatQuality.uppercase(), EmeraldSuccess)
                    InfoChip("النوع", if (task.formatMediaType == "video") "فيديو" else "صوت", Color(0xFF3B82F6))
                    InfoChip("الصيغة", task.formatExt.uppercase(), Color(0xFFA855F7))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("الحجم", color = DarkTextSecondary, fontSize = 11.sp)
                    Text(StorageHelper.formatBytes(fileSize), color = DarkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                if (task.completedAt != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("تاريخ التنزيل", color = DarkTextSecondary, fontSize = 11.sp)
                        Text(dateFormat.format(Date(task.completedAt)), color = DarkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("الحالة", color = DarkTextSecondary, fontSize = 11.sp)
                    if (fileExists) {
                        Text("✓ الملف موجود", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    } else {
                        Text("✗ الملف غير موجود", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Tools grid
        Text("الأدوات", color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

        // Play
        if (fileExists) {
            ToolCard(
                icon = Icons.Default.PlayArrow,
                title = "تشغيل",
                description = "افتح الملف في المشغل",
                color = EmeraldSuccess,
                onClick = { onPlay(task.outputPath!!, task.title, task.formatMediaType == "video") },
            )
        }

        // Share
        if (fileExists) {
            ToolCard(
                icon = Icons.Default.Share,
                title = "مشاركة",
                description = "شارك الملف مع تطبيقات أخرى",
                color = Color(0xFF3B82F6),
                onClick = { onShare(task.outputPath!!) },
            )
        }

        // Extract audio (video only)
        if (fileExists && task.formatMediaType == "video") {
            ToolCard(
                icon = Icons.Default.MusicNote,
                title = "استخراج الصوت",
                description = "استخرج المسار الصوتي من الفيديو",
                color = Color(0xFFA855F7),
                onClick = { /* TODO: FFmpeg integration */ },
            )
        }

        // Cut/Trim
        if (fileExists) {
            ToolCard(
                icon = Icons.Default.ContentCut,
                title = "قص",
                description = "اقتصاص جزء من الملف",
                color = Color(0xFFEF4444),
                onClick = { /* TODO: FFmpeg integration */ },
            )
        }

        // Compress
        if (fileExists) {
            ToolCard(
                icon = Icons.Default.Compress,
                title = "ضغط",
                description = "تقليل حجم الملف",
                color = AmberPrimary,
                onClick = { /* TODO: FFmpeg integration */ },
            )
        }

        // File info
        if (fileExists) {
            ToolCard(
                icon = Icons.Default.Info,
                title = "معلومات الملف",
                description = "تفاصيل تقنية عن الملف",
                color = DarkTextSecondary,
                onClick = { /* TODO: Show file details */ },
            )
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(label, color = DarkTextSecondary, fontSize = 8.sp)
            Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = DarkTextSecondary, fontSize = 10.sp)
            }
        }
    }
}
