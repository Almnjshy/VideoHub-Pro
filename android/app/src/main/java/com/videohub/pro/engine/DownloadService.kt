package com.videohub.pro.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.videohub.pro.MainActivity
import com.videohub.pro.R
import com.videohub.pro.data.database.VideoHubDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Download Foreground Service
 *
 * Lifecycle:
 *  - Started ONLY when a download task is created (via startWithTask)
 *  - Stops itself when no active download tasks remain
 *  - Never runs permanently when queue is empty
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var database: VideoHubDatabase
    @Inject lateinit var downloadEngine: DownloadEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engineJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID_DOWNLOADS = "downloads"
        const val CHANNEL_ID_EVENTS = "events"
        const val CHANNEL_ID_PLUGINS = "plugins"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.videohub.pro.START_DOWNLOADS"
        const val ACTION_STOP = "com.videohub.pro.STOP_DOWNLOADS"
        const val ACTION_CHECK_AND_STOP = "com.videohub.pro.CHECK_AND_STOP"

        /**
         * Start the service ONLY when a real download task exists.
         * Called by ShareViewModel.startDownload() after inserting a task.
         */
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Request the service to check if it should stop (no active tasks).
         */
        fun checkAndStop(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply { action = ACTION_CHECK_AND_STOP }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Force stop the service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        acquireWakeLock()
        // Must call startForeground within 5 seconds on Android 8+
        startForeground(NOTIFICATION_ID, buildNotification(0))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEngine()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CHECK_AND_STOP -> {
                scope.launch { checkAndMaybeStop() }
                return START_NOT_STICKY
            }
        }

        // Start engine loop if not already running
        if (engineJob == null || engineJob?.isActive != true) {
            startEngineLoop()
        }

        return START_STICKY
    }

    private fun startEngineLoop() {
        engineJob = scope.launch {
            var idleTicks = 0
            while (true) {
                val activeCount = downloadEngine.tick()

                if (activeCount > 0) {
                    // Active downloads — update notification with real progress
                    updateNotificationSuspend(activeCount)
                    idleTicks = 0
                } else {
                    idleTicks++
                    // After 2 idle ticks (0.5 second), immediately stop
                    if (idleTicks >= 2) {
                        // Cancel the notification before stopping
                        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        nm.cancel(NOTIFICATION_ID)
                        stopEngine()
                        stopSelf()
                        return@launch
                    }
                }

                delay(250)
            }
        }
    }

    private fun stopEngine() {
        engineJob?.cancel()
        engineJob = null
    }

    private suspend fun checkAndMaybeStop() {
        val activeCount = database.taskDao().getActiveCount()
        if (activeCount == 0) {
            stopEngine()
            stopSelf()
        } else {
            // Update notification with current state
            val notification = buildNotification(activeCount)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun updateNotificationSuspend(activeCount: Int) {
        val notification = buildNotification(activeCount)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(activeCount: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (activeCount > 0) {
            getString(R.string.download_service_active_count, activeCount)
        } else {
            getString(R.string.download_service_title)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID_DOWNLOADS)
            .setContentTitle(title)
            .setContentText(getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(activeCount > 0) // Only ongoing when actively downloading
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, activeCount > 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ID_DOWNLOADS,
            getString(R.string.notif_channel_downloads),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_downloads_desc)
            setShowBadge(false)
        })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ID_EVENTS,
            getString(R.string.notif_channel_events),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notif_channel_events_desc)
        })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ID_PLUGINS,
            getString(R.string.notif_channel_plugins),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notif_channel_plugins_desc)
        })
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VideoHubPro::DownloadWakeLock",
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L) // 10 minutes max, renewed by engine
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEngine()
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
