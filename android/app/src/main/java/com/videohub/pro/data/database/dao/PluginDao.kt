package com.videohub.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.videohub.pro.data.database.entities.PluginEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PluginDao {

    @Query("SELECT * FROM plugins ORDER BY name")
    fun observeAll(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE id = :id")
    suspend fun getById(id: String): PluginEntity?

    @Query("SELECT * FROM plugins WHERE enabled = 1")
    suspend fun getEnabled(): List<PluginEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plugin: PluginEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plugins: List<PluginEntity>)

    @Update
    suspend fun update(plugin: PluginEntity)

    @Query("UPDATE plugins SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE plugins SET version = :version, status = 'healthy', failedAttempts = 0, successRate = 1.0, lastErrorJson = NULL WHERE id = :id")
    suspend fun updateVersion(id: String, version: String)

    @Query("UPDATE plugins SET totalAttempts = totalAttempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: String)

    @Query("UPDATE plugins SET successfulAttempts = successfulAttempts + 1, lastSuccessAt = :timestamp WHERE id = :id")
    suspend fun incrementSuccess(id: String, timestamp: Long)

    @Query("UPDATE plugins SET failedAttempts = failedAttempts + 1, lastErrorJson = :errorJson WHERE id = :id")
    suspend fun incrementFailed(id: String, errorJson: String)

    @Query("UPDATE plugins SET successRate = :rate, status = :status WHERE id = :id")
    suspend fun updateHealth(id: String, rate: Float, status: String)

    @Query("UPDATE plugins SET lastAutoTestAt = :lastTest, nextAutoTestAt = :nextTest WHERE id = :id")
    suspend fun updateAutoTestTimes(id: String, lastTest: Long, nextTest: Long)

    @Query("SELECT COUNT(*) FROM plugins")
    suspend fun count(): Int
}
