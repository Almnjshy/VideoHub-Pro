package com.videohub.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videohub.pro.data.database.entities.AutoTestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoTestResultDao {

    @Query("SELECT * FROM auto_test_results ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<AutoTestResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: AutoTestResultEntity)

    @Query("DELETE FROM auto_test_results WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
