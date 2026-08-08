package com.videohub.pro.ui.screens.stats

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.videohub.pro.ui.theme.EmeraldSuccess

@Composable
fun StatsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("الإحصائيات", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Top stats (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Download,
                label = "إجمالي التنزيلات",
                value = "47",
                accent = AmberPrimary,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                label = "مكتملة",
                value = "41",
                accent = EmeraldSuccess,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                label = "متوسط السرعة",
                value = "4.2",
                sub = "MB/s",
                accent = Color(0xFF3B82F6),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                label = "معدل النجاح",
                value = "87%",
                accent = AmberPrimary,
            )
        }

        // Platform distribution bar chart (simulated)
        Surface(
            color = DarkBgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("توزيع التنزيلات حسب المنصة", color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                val platforms = listOf(
                    "يوتيوب" to 0.6f to AmberPrimary,
                    "تيك توك" to 0.25f to Color(0xFFFE2C55),
                    "فيسبوك" to 0.1f to Color(0xFF1877F2),
                    "إنستغرام" to 0.05f to Color(0xFFE4405F),
                )

                platforms.forEach { (pair, color) ->
                    val (name, pct) = pair
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(name, color = DarkTextSecondary, fontSize = 11.sp)
                            Text("${(pct * 100).toInt()}%", color = DarkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(DarkBorder),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pct)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color),
                            )
                        }
                    }
                }
            }
        }

        // Activity timeline (simulated)
        Surface(
            color = DarkBgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("النشاط خلال آخر 12 ساعة", color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                val activity = listOf(2, 3, 1, 4, 2, 5, 3, 2, 4, 1, 3, 2)
                val maxActivity = activity.max()

                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    activity.forEach { count ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((count.toFloat() / maxActivity * 80).dp)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(AmberPrimary.copy(alpha = 0.7f)),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("12س", color = DarkBorder, fontSize = 9.sp)
                    Text("الآن", color = DarkBorder, fontSize = 9.sp)
                }
            }
        }

        // Quick stats
        Surface(
            color = DarkBgCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("معلومات إضافية", color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                InfoRow("إجمالي البيانات المنزّلة", "11.8 GB")
                InfoRow("عدد الملفات", "41")
                InfoRow("المساحة المستخدمة", "37%")
                InfoRow("الوحدات النشطة", "14 / 14")
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    sub: String = "",
    accent: Color,
) {
    Surface(
        color = DarkBgCard,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = DarkTextSecondary, fontSize = 10.sp)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (sub.isNotEmpty()) Text(sub, color = DarkTextSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = DarkTextSecondary, fontSize = 12.sp)
        Text(value, color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
