package com.videohub.pro.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Task Entity — سجل المهام الكامل
 */
@Entity(tableName = "tasks", indices = [
    androidx.room.Index("status"),
    androidx.room.Index("platformId"),
    androidx.room.Index("priority"),
    androidx.room.Index("createdAt"),
])
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sourceUrl") val sourceUrl: String,
    @ColumnInfo(name = "platformId") val platformId: String,
    val title: String,
    val author: String? = null,
    @ColumnInfo(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @ColumnInfo(name = "durationSeconds") val durationSeconds: Int? = null,
    val description: String? = null,
    @ColumnInfo(name = "formatId") val formatId: String,
    @ColumnInfo(name = "formatQuality") val formatQuality: String,
    @ColumnInfo(name = "formatExt") val formatExt: String,
    @ColumnInfo(name = "formatSizeBytes") val formatSizeBytes: Long,
    @ColumnInfo(name = "formatMediaType") val formatMediaType: String,
    val status: String = "queued",
    val progress: Float = 0f,
    @ColumnInfo(name = "downloadedBytes") val downloadedBytes: Long = 0L,
    @ColumnInfo(name = "totalBytes") val totalBytes: Long,
    @ColumnInfo(name = "speedBps") val speedBps: Long = 0L,
    @ColumnInfo(name = "etaSeconds") val etaSeconds: Int = 0,
    val retries: Int = 0,
    @ColumnInfo(name = "maxRetries") val maxRetries: Int = 3,
    val priority: Int = 1,
    val error: String? = null,
    @ColumnInfo(name = "errorStage") val errorStage: String? = null,
    @ColumnInfo(name = "outputPath") val outputPath: String? = null,
    @ColumnInfo(name = "downloadUrl") val downloadUrl: String? = null,
    @ColumnInfo(name = "segmentsJson") val segmentsJson: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "startedAt") val startedAt: Long? = null,
    @ColumnInfo(name = "completedAt") val completedAt: Long? = null,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
)
