package com.videohub.pro.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.videohub.pro.data.database.dao.AppStatDao
import com.videohub.pro.data.database.dao.AutoTestResultDao
import com.videohub.pro.data.database.dao.FaultReportDao
import com.videohub.pro.data.database.dao.NotificationDao
import com.videohub.pro.data.database.dao.PluginDao
import com.videohub.pro.data.database.dao.TaskDao
import com.videohub.pro.data.database.entities.AppStatEntity
import com.videohub.pro.data.database.entities.AutoTestResultEntity
import com.videohub.pro.data.database.entities.FaultReportEntity
import com.videohub.pro.data.database.entities.NotificationEntity
import com.videohub.pro.data.database.entities.PluginEntity
import com.videohub.pro.data.database.entities.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        PluginEntity::class,
        FaultReportEntity::class,
        AutoTestResultEntity::class,
        NotificationEntity::class,
        AppStatEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class VideoHubDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun pluginDao(): PluginDao
    abstract fun faultReportDao(): FaultReportDao
    abstract fun autoTestResultDao(): AutoTestResultDao
    abstract fun notificationDao(): NotificationDao
    abstract fun appStatDao(): AppStatDao

    companion object {
        const val DATABASE_NAME = "videohub.db"
    }
}
