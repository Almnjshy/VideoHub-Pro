package com.videohub.pro.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Plugin Entity — سجل الوحدات المثبتة وحالتها الصحية
 */
@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "nameAr") val nameAr: String,
    val icon: String,
    val color: String,
    val version: String,
    val enabled: Boolean = true,
    @ColumnInfo(name = "totalAttempts") val totalAttempts: Int = 0,
    @ColumnInfo(name = "successfulAttempts") val successfulAttempts: Int = 0,
    @ColumnInfo(name = "failedAttempts") val failedAttempts: Int = 0,
    @ColumnInfo(name = "successRate") val successRate: Float = 1f,
    val status: String = "healthy", // healthy, degraded, broken
    @ColumnInfo(name = "lastErrorJson") val lastErrorJson: String? = null,
    @ColumnInfo(name = "lastSuccessAt") val lastSuccessAt: Long? = null,
    @ColumnInfo(name = "lastAutoTestAt") val lastAutoTestAt: Long? = null,
    @ColumnInfo(name = "nextAutoTestAt") val nextAutoTestAt: Long? = null,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
)
