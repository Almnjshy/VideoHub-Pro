package com.videohub.pro.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auto_test_results",
    indices = [Index("pluginId"), Index("timestamp")],
)
data class AutoTestResultEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "pluginId") val pluginId: String,
    @ColumnInfo(name = "testName") val testName: String,
    val passed: Boolean,
    @ColumnInfo(name = "durationMs") val durationMs: Int,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
