package com.videohub.pro.diagnostics

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Global Crash Reporter — يلتقط أي استثناء غير معالَج ويعرضه كإشعار محلي.
 *
 * - لا يستخدم أي خدمة خارجية (لا Firebase، لا إنترنت).
 * - يعرض السبب الحقيقي للمستخدم/المطوّر قبل ما يكمل النظام تعطيل التطبيق.
 * - لا يمنع الكراش ولا يستبدل معالجة الأخطاء الطبيعية.
 */
object CrashReporter {

    private const val CHANNEL_ID = "crash_reports"
    private const val NOTIFICATION_ID = 9001
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true

        val appContext = context.applicationContext
        createNotificationChannel(appContext)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { postCrashNotification(appContext, throwable) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Crash reports", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Shows the real exception when the app crashes, for debugging."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun postCrashNotification(context: Context, throwable: Throwable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        val fullTrace = stringWriter.toString()
        val summary = "${throwable::class.java.name}: ${throwable.message}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("App crashed")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$summary\n\n${fullTrace.take(3000)}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }
}
