package com.videohub.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.videohub.pro.data.database.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT 50")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notifications SET read = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
