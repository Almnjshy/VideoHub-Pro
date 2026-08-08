package com.videohub.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videohub.pro.data.database.entities.FaultReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FaultReportDao {

    @Query("SELECT * FROM fault_reports ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<FaultReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: FaultReportEntity)

    @Query("DELETE FROM fault_reports")
    suspend fun deleteAll()

    @Query("UPDATE fault_reports SET resolved = 1 WHERE pluginId = :pluginId")
    suspend fun resolveByPlugin(pluginId: String)
}
