package com.videohub.pro.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Media Playback Service — لتشغيل الوسائط في الخلفية
 * يستخدم Media3 (ExoPlayer) + MediaSession
 *
 * ملاحظة: في media3 1.5+، @UnstableApi يُستخدم مباشرة كـ annotation
 * (وليس عبر @OptIn(UnstableApi::class))
 */
@UnstableApi
class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
