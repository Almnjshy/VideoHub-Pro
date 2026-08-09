package com.videohub.pro.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.videohub.pro.MainActivity
import com.videohub.pro.R
import com.videohub.pro.engine.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Helper — نظام الإشعارات الأصلي
 * يتعامل مع 3 قنوات: التنزيلات، الأحداث، الوحدات
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun showTaskCompletedNotification(taskId: String, title: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", taskId)
            putExtra("action", "open_downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, DownloadService.CHANNEL_ID_EVENTS)
            .setContentTitle("✓ اكتمل التنزيل")
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }

    fun showTaskFailedNotification(taskId: String, title: String, error: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", taskId)
            putExtra("action", "open_downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, DownloadService.CHANNEL_ID_EVENTS)
            .setContentTitle("✗ فشل التنزيل")
            .setContentText("$title — $error")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }

    fun showPluginHealthNotification(pluginName: String, status: String, successRate: Int) {
        val title = if (status == "broken") "⚠ وحدة $pluginName معطلة" else "⚠ وحدة $pluginName متدهورة"
        val text = "انخفض معدل نجاح الوحدة إلى $successRate%"

        val notification = NotificationCompat.Builder(context, DownloadService.CHANNEL_ID_PLUGINS)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(pluginName.hashCode(), notification)
    }

    fun showStorageWarning(usagePct: Int) {
        val notification = NotificationCompat.Builder(context, DownloadService.CHANNEL_ID_EVENTS)
            .setContentTitle("تحذير: مساحة التخزين منخفضة")
            .setContentText("تم استخدام $usagePct% من المساحة المتاحة")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify("storage_warning".hashCode(), notification)
    }

    /**
     * Build a foreground notification for the download service with REAL progress.
     *
     * @param title Task title
     * @param progress 0-100
     * @param speedBytesPerSec Download speed in bytes/sec
     * @param etaSeconds Estimated time remaining in seconds
     * @param activeCount Number of active downloads
     */
    fun buildDownloadProgressNotification(
        title: String,
        progress: Int,
        speedBytesPerSec: Long,
        etaSeconds: Int,
        activeCount: Int,
    ): android.app.Notification {
        val speedStr = formatSpeed(speedBytesPerSec)
        val etaStr = formatEta(etaSeconds)
        val contentText = if (activeCount > 1) {
            "$activeCount تنزيلات نشطة · $speedStr · $etaStr"
        } else {
            "$progress% · $speedStr · $etaStr"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "open_downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, DownloadService.CHANNEL_ID_DOWNLOADS)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Build a foreground notification for when downloads are queued but not yet active.
     */
    fun buildDownloadIdleNotification(queuedCount: Int): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "open_downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, DownloadService.CHANNEL_ID_DOWNLOADS)
            .setContentTitle("VideoHub Pro")
            .setContentText(if (queuedCount > 0) "$queuedCount في قائمة الانتظار" else "جاهز للتنزيل")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "—"
        if (bytesPerSec < 1024) return "$bytesPerSec B/s"
        if (bytesPerSec < 1024 * 1024) return "${bytesPerSec / 1024} KB/s"
        return String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
    }

    private fun formatEta(seconds: Int): String {
        if (seconds <= 0) return "—"
        if (seconds < 60) return "${seconds}s"
        val m = seconds / 60
        val s = seconds % 60
        return "${m}:${String.format("%02d", s)}"
    }
}
