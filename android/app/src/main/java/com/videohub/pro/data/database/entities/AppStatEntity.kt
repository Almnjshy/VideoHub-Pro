package com.videohub.pro.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_stats")
data class AppStatEntity(
    @PrimaryKey val id: String = "singleton",
    @ColumnInfo(name = "totalDownloads") val totalDownloads: Int = 0,
    @ColumnInfo(name = "completedDownloads") val completedDownloads: Int = 0,
    @ColumnInfo(name = "failedDownloads") val failedDownloads: Int = 0,
    @ColumnInfo(name = "totalBytesDownloaded") val totalBytesDownloaded: Long = 0L,
    @ColumnInfo(name = "averageSpeed") val averageSpeed: Long = 0L,
    @ColumnInfo(name = "storageUsedBytes") val storageUsedBytes: Long = 0L,
    @ColumnInfo(name = "storageLimitBytes") val storageLimitBytes: Long = 32L * 1024 * 1024 * 1024,
    @ColumnInfo(name = "fileCount") val fileCount: Int = 0,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
)
