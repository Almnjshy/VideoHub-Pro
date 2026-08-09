package com.videohub.pro.engine

import android.content.Context
import android.os.Environment
import android.util.Log
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.data.database.entities.AppStatEntity
import com.videohub.pro.data.database.entities.FaultReportEntity
import com.videohub.pro.data.database.entities.NotificationEntity
import com.videohub.pro.domain.models.ErrorStage
import com.videohub.pro.domain.models.PluginErrorType
import com.videohub.pro.domain.models.TaskStatus
import com.videohub.pro.network.DownloadResult
import com.videohub.pro.network.NetworkClient
import com.videohub.pro.notifications.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REAL Download Engine — downloads actual bytes from the internet.
 *
 * Pipeline:
 *  1. Get the task from DB
 *  2. Get the real downloadUrl from the task
 *  3. Use NetworkClient.downloadFile() to download REAL bytes
 *  4. Track REAL progress (actual bytes received / total bytes)
 *  5. Write REAL bytes to file
 *  6. Verify file exists and size > 0
 *  7. Only mark as completed if file verification passes
 *
 * NO simulated progress. NO fake bytes. NO artificial delays.
 */
@Singleton
class DownloadEngine @Inject constructor(
    private val database: VideoHubDatabase,
    private val notificationHelper: NotificationHelper,
    @ApplicationContext private val context: Context,
    private val networkClient: NetworkClient,
    private val appSettings: com.videohub.pro.data.AppSettings,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Process one tick of the download engine.
     * Returns the number of active tasks.
     */
    suspend fun tick(): Int = withContext(Dispatchers.IO) {
        val settings = getSettings()
        val now = System.currentTimeMillis()

        // 1. Promote queued tasks to downloading
        promoteQueuedTasks(settings, now)

        // 2. Process downloading tasks — REAL downloads
        processDownloadingTasks(settings, now)

        // 3. Return active count
        val downloading = database.taskDao().getDownloading().size
        val queued = database.taskDao().getQueued(100).size
        downloading + queued
    }

    private suspend fun promoteQueuedTasks(settings: AppSettings, now: Long) {
        // Read concurrent limit from AppSettings (real, saved by user)
        val maxConcurrent = appSettings.getConcurrentDownloads()
        val activeTasks = database.taskDao().getDownloading()
        val availableSlots = maxOf(0, maxConcurrent - activeTasks.size)
        if (availableSlots == 0) return

        val queued = database.taskDao().getQueued(availableSlots)

        for (task in queued) {
            // Mark as downloading
            database.taskDao().update(
                task.copy(
                    status = TaskStatus.DOWNLOADING.name.lowercase(),
                    startedAt = now,
                    updatedAt = now,
                ),
            )
            Log.i(TAG, "Task promoted to downloading: ${task.title}, downloadUrl=${task.downloadUrl}")
        }
    }

    /**
     * Process downloading tasks — perform REAL HTTP downloads.
     */
    private suspend fun processDownloadingTasks(settings: AppSettings, now: Long) {
        val downloading = database.taskDao().getDownloading()
        if (downloading.isEmpty()) return

        for (task in downloading) {
            // Get the REAL download URL
            val downloadUrl = task.downloadUrl
            if (downloadUrl.isNullOrBlank()) {
                // No download URL — mark as failed
                Log.e(TAG, "Task ${task.id} has no downloadUrl — FAILING")
                database.taskDao().update(
                    task.copy(
                        status = TaskStatus.FAILED.name.lowercase(),
                        error = "No download URL available for this media",
                        errorStage = ErrorStage.RESOLVE_LINKS.name.lowercase(),
                        updatedAt = now,
                    ),
                )
                continue
            }

            // Determine output file path
            val subFolder = if (task.formatMediaType == "audio") settings.audioPath else settings.videoPath
            val outputDir = File(subFolder)
            if (!outputDir.exists()) outputDir.mkdirs()

            val safeTitle = task.title.replace(Regex("[^\\wآ-ي]+"), "_").take(50)
            val outputFile = File(outputDir, "$safeTitle.${task.formatExt}")

            Log.i(TAG, "Starting REAL download: $downloadUrl → ${outputFile.absolutePath}")

            // Update task to show downloading has started
            database.taskDao().updateProgress(
                id = task.id,
                progress = 0f,
                downloaded = 0L,
                speed = 0L,
                eta = 0,
                segments = "[]",
            )

            // Perform REAL download
            val result = networkClient.downloadFile(
                url = downloadUrl,
                outputFile = outputFile,
                onProgress = { downloadedBytes, totalBytes ->
                    // Update progress in DB with REAL numbers — use runBlocking for suspend call in callback
                    val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                    val speed = downloadedBytes

                    // Only update DB every ~200KB to avoid excessive writes
                    if (downloadedBytes % 200_000 < 8192 || (totalBytes > 0 && downloadedBytes >= totalBytes)) {
                        try {
                            val eta = if (totalBytes > 0 && downloadedBytes > 0) {
                                ((totalBytes - downloadedBytes) * 1000 / maxOf(1, downloadedBytes)).toInt()
                            } else 0

                            kotlinx.coroutines.runBlocking {
                                database.taskDao().updateProgress(
                                    id = task.id,
                                    progress = progress,
                                    downloaded = downloadedBytes,
                                    speed = speed,
                                    eta = eta,
                                    segments = "[]",
                                )
                            }
                        } catch (e: Exception) {
                            // DB update failure is non-critical
                        }
                    }
                },
            )

            // Handle download result
            when (result) {
                is DownloadResult.Success -> {
                    Log.i(TAG, "Download SUCCESS: ${result.filePath}, size=${result.fileSize} bytes")

                    // Verify file exists one more time
                    val file = File(result.filePath)
                    if (!file.exists() || file.length() == 0L) {
                        Log.e(TAG, "File verification FAILED — file doesn't exist or is empty")
                        database.taskDao().update(
                            task.copy(
                                status = TaskStatus.FAILED.name.lowercase(),
                                error = "File verification failed — file is empty or missing",
                                errorStage = ErrorStage.FINALIZE.name.lowercase(),
                                speedBps = 0L,
                                etaSeconds = 0,
                                updatedAt = now,
                            ),
                        )
                        notificationHelper.showTaskFailedNotification(task.id, task.title, "File verification failed")
                    } else {
                        // SUCCESS — mark as completed
                        database.taskDao().update(
                            task.copy(
                                status = TaskStatus.COMPLETED.name.lowercase(),
                                progress = 1f,
                                downloadedBytes = result.fileSize,
                                totalBytes = result.fileSize,
                                speedBps = 0L,
                                etaSeconds = 0,
                                completedAt = now,
                                outputPath = result.filePath,
                                updatedAt = now,
                            ),
                        )

                        // Update stats
                        val stats = database.appStatDao().get() ?: AppStatEntity()
                        database.appStatDao().update(
                            stats.copy(
                                totalDownloads = stats.totalDownloads + 1,
                                completedDownloads = stats.completedDownloads + 1,
                                totalBytesDownloaded = stats.totalBytesDownloaded + result.fileSize,
                                storageUsedBytes = stats.storageUsedBytes + result.fileSize,
                                fileCount = stats.fileCount + 1,
                                updatedAt = now,
                            ),
                        )

                        // Notification
                        database.notificationDao().insert(
                            NotificationEntity(
                                id = UUID.randomUUID().toString(),
                                type = "task_completed",
                                title = "اكتمل التنزيل",
                                message = "${task.title} (${result.fileSize} bytes)",
                                taskId = task.id,
                                timestamp = now,
                            ),
                        )
                        notificationHelper.showTaskCompletedNotification(task.id, task.title)
                    }
                }

                is DownloadResult.Error -> {
                    Log.e(TAG, "Download FAILED: ${result.message}")

                    // Clean up partial file
                    if (outputFile.exists()) outputFile.delete()

                    database.taskDao().update(
                        task.copy(
                            status = TaskStatus.FAILED.name.lowercase(),
                            error = result.message,
                            errorStage = ErrorStage.DOWNLOAD_SEGMENT.name.lowercase(),
                            speedBps = 0L,
                            etaSeconds = 0,
                            updatedAt = now,
                        ),
                    )

                    database.notificationDao().insert(
                        NotificationEntity(
                            id = UUID.randomUUID().toString(),
                            type = "task_failed",
                            title = "فشل التنزيل",
                            message = "${task.title} — ${result.message}",
                            taskId = task.id,
                            pluginId = task.platformId,
                            timestamp = now,
                        ),
                    )
                    notificationHelper.showTaskFailedNotification(task.id, task.title, result.message)
                }
            }
        }
    }

    /**
     * Build initial segments JSON (kept for DB compatibility, but not used for fake progress).
     */
    fun buildInitialSegments(totalBytes: Long): String {
        return "[]"
    }

    // ============ Settings (read from real AppSettings) ============

    data class AppSettings(
        val concurrentDownloads: Int = 3,
        val concurrentPerDomain: Int = 2,
        val smartScheduling: Boolean = true,
        val bandwidthLimitMbps: Int = 0,
        val autoRetry: Boolean = true,
        val maxRetries: Int = 3,
        val downloadPath: String = "",
        val videoPath: String = "",
        val audioPath: String = "",
    )

    private suspend fun getSettings(): AppSettings {
        val basePath = File(context.getExternalFilesDir(null), "VideoHub Pro").apply { if (!exists()) mkdirs() }
        val videoDir = File(basePath, "Video").apply { if (!exists()) mkdirs() }
        val audioDir = File(basePath, "Audio").apply { if (!exists()) mkdirs() }
        return AppSettings(
            concurrentDownloads = appSettings.getConcurrentDownloads(),
            autoRetry = appSettings.getAutoRetry(),
            maxRetries = 3,
            downloadPath = basePath.absolutePath + File.separator,
            videoPath = videoDir.absolutePath + File.separator,
            audioPath = audioDir.absolutePath + File.separator,
        )
    }

    companion object {
        private const val TAG = "DownloadEngine"
    }
}
