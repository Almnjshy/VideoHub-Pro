package com.videohub.pro.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [Index("read"), Index("timestamp")],
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val message: String,
    @ColumnInfo(name = "taskId") val taskId: String? = null,
    @ColumnInfo(name = "pluginId") val pluginId: String? = null,
    val read: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)
