package com.videohub.pro.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fault Report Entity — تقارير الأعطال
 */
@Entity(
    tableName = "fault_reports",
    indices = [Index("pluginId"), Index("timestamp")],
)
data class FaultReportEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "pluginId") val pluginId: String,
    val stage: String,
    @ColumnInfo(name = "errorType") val errorType: String,
    val message: String,
    @ColumnInfo(name = "taskId") val taskId: String? = null,
    val resolved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)
