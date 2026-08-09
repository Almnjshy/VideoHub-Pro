package com.videohub.pro.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

/**
 * StorageHelper — provides real device storage information.
 * Uses StatFs to get actual filesystem stats instead of hardcoded values.
 */
object StorageHelper {

    /**
     * Get real storage info for the app's external files directory.
     * Returns StorageInfo with actual device storage stats.
     */
    fun getStorageInfo(context: Context): StorageInfo {
        return try {
            val downloadDir = File(
                context.getExternalFilesDir(null),
                "VideoHub Pro"
            )
            val partitionRoot = downloadDir.parentFile?.parentFile?.parentFile?.parentFile
                ?: Environment.getExternalStorageDirectory()

            val stat = StatFs(partitionRoot.absolutePath)
            val totalBytes = stat.totalBytes
            val availableBytes = stat.availableBytes
            val usedBytes = totalBytes - availableBytes

            // Count actual files and their sizes in the app download directory
            var appUsedBytes = 0L
            var fileCount = 0
            if (downloadDir.exists()) {
                downloadDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        appUsedBytes += file.length()
                        fileCount++
                    }
                }
            }

            StorageInfo(
                appUsedBytes = appUsedBytes,
                totalBytes = totalBytes,
                availableBytes = availableBytes,
                partitionUsedBytes = usedBytes,
                fileCount = fileCount,
            )
        } catch (e: Exception) {
            StorageInfo(
                appUsedBytes = 0L,
                totalBytes = 32L * 1024 * 1024 * 1024,
                availableBytes = 32L * 1024 * 1024 * 1024,
                partitionUsedBytes = 0L,
                fileCount = 0,
            )
        }
    }

    /**
     * Format bytes to human-readable string (e.g., "11.8 GB").
     */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${String.format("%.1f", bytes / 1024.0)} KB"
        if (bytes < 1024L * 1024 * 1024) return "${String.format("%.1f", bytes / (1024.0 * 1024))} MB"
        return "${String.format("%.1f", bytes / (1024.0 * 1024 * 1024))} GB"
    }

    /**
     * Format bytes to short string (e.g., "11.8GB" without space).
     */
    fun formatBytesShort(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        if (bytes < 1024 * 1024) return "${(bytes / 1024).toInt()}KB"
        if (bytes < 1024L * 1024 * 1024) return "${String.format("%.1f", bytes / (1024.0 * 1024))}MB"
        return "${String.format("%.1f", bytes / (1024.0 * 1024 * 1024))}GB"
    }

    data class StorageInfo(
        val appUsedBytes: Long,
        val totalBytes: Long,
        val availableBytes: Long,
        val partitionUsedBytes: Long,
        val fileCount: Int,
    ) {
        val usagePercent: Float
            get() = if (totalBytes > 0) (partitionUsedBytes.toFloat() / totalBytes) else 0f
    }
}
