package com.videohub.pro.ui.player

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.videohub.pro.ui.theme.AmberPrimary
import com.videohub.pro.ui.theme.DarkBgPrimary
import com.videohub.pro.ui.theme.DarkBgSecondary
import com.videohub.pro.ui.theme.DarkBorder
import com.videohub.pro.ui.theme.DarkTextPrimary
import com.videohub.pro.ui.theme.DarkTextSecondary
import java.io.File

/**
 * Full-screen media player using ExoPlayer (Media3).
 * Supports both video and audio playback.
 * Includes "Open in external player" option.
 */
@UnstableApi
@Composable
fun MediaPlayerScreen(
    filePath: String,
    title: String,
    isVideo: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }

    // Create and configure ExoPlayer
    DisposableEffect(filePath) {
        val exoPlayer = ExoPlayer.Builder(context).build()
        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        player = exoPlayer

        onDispose {
            exoPlayer.release()
            player = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (isVideo) {
            // Video player view
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.player = player
                },
            )
        } else {
            // Audio player — show title + controls
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Album art placeholder
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(AmberPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎵", fontSize = 64.sp)
                }
                Spacer(Modifier.height(24.dp))
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))

                // Play/Pause button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AmberPrimary, RoundedCornerShape(32.dp))
                        .clickable {
                            player?.let { p ->
                                if (p.isPlaying) {
                                    p.pause()
                                    isPlaying = false
                                } else {
                                    p.play()
                                    isPlaying = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        // Top bar with close + open external
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Close button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

            // Open in external player
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        openInExternalPlayer(context, filePath, isVideo)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "فتح بمشغل خارجي", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        // Open external button at bottom
        Surface(
            color = DarkBgSecondary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .clickable {
                    openInExternalPlayer(context, filePath, isVideo)
                },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                Text("فتح بمشغل آخر", color = AmberPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Open a media file in an external Android player (VLC, MX Player, system player, etc.)
 */
private fun openInExternalPlayer(context: android.content.Context, filePath: String, isVideo: Boolean) {
    try {
        val file = File(filePath)
        val authority = context.packageName + ".fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val mimeType = when {
            isVideo -> "video/*"
            else -> "audio/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "فتح باستخدام"))
    } catch (e: Exception) {
        // Fallback: try without FileProvider
        try {
            val file = File(filePath)
            val uri = Uri.fromFile(file)
            val mimeType = if (isVideo) "video/*" else "audio/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "فتح باستخدام"))
        } catch (e2: Exception) {
            // Silently fail
        }
    }
}
