package com.videohub.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.videohub.pro.data.database.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority ASC, createdAt ASC")
    fun observeByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = 'queued' ORDER BY priority ASC, createdAt ASC LIMIT :limit")
    suspend fun getQueued(limit: Int): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'downloading'")
    suspend fun getDownloading(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'failed' AND retries < :maxRetries LIMIT :limit")
    suspend fun getRetryable(maxRetries: Int, limit: Int): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'downloading'")
    suspend fun getActiveCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'downloading' AND platformId = :platformId")
    suspend fun getActiveCountForPlatform(platformId: String): Int

    @Query("SELECT platformId, COUNT(*) as count FROM tasks GROUP BY platformId ORDER BY count DESC LIMIT 5")
    suspend fun getTopPlatforms(): List<PlatformCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE tasks SET status = :status, error = :error, errorStage = :stage WHERE id = :id")
    suspend fun updateError(id: String, status: String, error: String?, stage: String?)

    @Query("UPDATE tasks SET progress = :progress, downloadedBytes = :downloaded, speedBps = :speed, etaSeconds = :eta, segmentsJson = :segments WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, downloaded: Long, speed: Long, eta: Int, segments: String)

    @Query("UPDATE tasks SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: String, priority: Int)

    @Query("UPDATE tasks SET retries = retries + 1, status = 'queued', error = NULL, errorStage = NULL WHERE id = :id")
    suspend fun incrementRetry(id: String)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks WHERE status = 'completed'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    data class PlatformCount(val platformId: String, val count: Int)
}
