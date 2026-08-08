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
}
