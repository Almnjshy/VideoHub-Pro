package com.videohub.pro.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.videohub.pro.data.database.dao.AppStatDao
import com.videohub.pro.data.database.dao.AutoTestResultDao
import com.videohub.pro.data.database.dao.FaultReportDao
import com.videohub.pro.data.database.dao.FavoriteDao
import com.videohub.pro.data.database.dao.NotificationDao
import com.videohub.pro.data.database.dao.PluginDao
import com.videohub.pro.data.database.dao.SearchHistoryDao
import com.videohub.pro.data.database.dao.TaskDao
import com.videohub.pro.data.database.entities.AppStatEntity
import com.videohub.pro.data.database.entities.AutoTestResultEntity
import com.videohub.pro.data.database.entities.FaultReportEntity
import com.videohub.pro.data.database.entities.FavoriteEntity
import com.videohub.pro.data.database.entities.NotificationEntity
import com.videohub.pro.data.database.entities.PluginEntity
import com.videohub.pro.data.database.entities.SearchHistoryEntity
import com.videohub.pro.data.database.entities.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        PluginEntity::class,
        FaultReportEntity::class,
        AutoTestResultEntity::class,
        NotificationEntity::class,
        AppStatEntity::class,
        FavoriteEntity::class,
        SearchHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class VideoHubDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun pluginDao(): PluginDao
    abstract fun faultReportDao(): FaultReportDao
    abstract fun autoTestResultDao(): AutoTestResultDao
    abstract fun notificationDao(): NotificationDao
    abstract fun appStatDao(): AppStatDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        const val DATABASE_NAME = "videohub.db"

        @Volatile
        private var INSTANCE: VideoHubDatabase? = null

        fun getInstance(context: android.content.Context): VideoHubDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VideoHubDatabase::class.java,
                    DATABASE_NAME,
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
