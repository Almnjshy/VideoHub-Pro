package com.videohub.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.videohub.pro.data.database.entities.AppStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppStatDao {

    @Query("SELECT * FROM app_stats WHERE id = 'singleton'")
    suspend fun get(): AppStatEntity?

    @Query("SELECT * FROM app_stats WHERE id = 'singleton'")
    fun observe(): Flow<AppStatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stat: AppStatEntity)

    @Update
    suspend fun update(stat: AppStatEntity)

    @Query("UPDATE app_stats SET totalDownloads = totalDownloads + :total, completedDownloads = completedDownloads + :completed, failedDownloads = failedDownloads + :failed, totalBytesDownloaded = totalBytesDownloaded + :bytes, fileCount = fileCount + :files, storageUsedBytes = storageUsedBytes + :storageBytes, updatedAt = :timestamp WHERE id = 'singleton'")
    suspend fun incrementStats(total: Int, completed: Int, failed: Int, bytes: Long, files: Int, storageBytes: Long, timestamp: Long)
}
