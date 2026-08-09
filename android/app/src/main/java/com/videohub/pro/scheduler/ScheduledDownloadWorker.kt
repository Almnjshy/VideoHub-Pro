package com.videohub.pro.scheduler

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Download Scheduler — يتيح جدولة التنزيلات لوقت محدد.
 * مثال: تنزيل في الساعة 2 صباحاً لتوفير البيانات.
 */
class ScheduledDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val taskId = inputData.getString("taskId") ?: java.util.UUID.randomUUID().toString()

        // The DownloadService will pick up this URL from a shared preferences queue
        val prefs = getApplicationContext().getSharedPreferences("scheduled_downloads", Context.MODE_PRIVATE)
        val queue = prefs.getStringSet("queue", mutableSetOf()) ?: mutableSetOf()
        prefs.edit().putStringSet("queue", queue + url).apply()

        // Start the download service
        val intent = android.content.Intent(applicationContext, com.videohub.pro.engine.DownloadService::class.java)
        intent.action = com.videohub.pro.engine.DownloadService.ACTION_START
        androidx.core.content.ContextCompat.startForegroundService(applicationContext, intent)

        return Result.success()
    }

    companion object {
        /**
         * Schedule a download to run after a delay.
         *
         * @param context Context
         * @param url URL to download
         * @param delayMinutes Minutes to wait before downloading
         */
        fun scheduleDownload(context: Context, url: String, delayMinutes: Long) {
            val data = Data.Builder()
                .putString("url", url)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi only
                .setRequiresBatteryNotLow(true)
                .build()

            val request = OneTimeWorkRequestBuilder<ScheduledDownloadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /**
         * Schedule a download at a specific hour (e.g., 2 AM).
         *
         * @param context Context
         * @param url URL to download
         * @param targetHour Hour of day (0-23)
         */
        fun scheduleAtTime(context: Context, url: String, targetHour: Int) {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                set(java.util.Calendar.HOUR_OF_DAY, targetHour)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                if (timeInMillis <= now) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            val delayMinutes = (calendar.timeInMillis - now) / (60 * 1000)
            scheduleDownload(context, url, delayMinutes)
        }
    }
}
