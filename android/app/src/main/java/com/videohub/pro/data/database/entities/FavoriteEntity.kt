package com.videohub.pro.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorites", indices = [Index("url", unique = true)])
data class FavoriteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "uploader") val uploader: String? = null,
    @ColumnInfo(name = "thumbnail") val thumbnail: String? = null,
    @ColumnInfo(name = "duration") val duration: Int? = null,
    @ColumnInfo(name = "platform") val platform: String? = null,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "search_history", indices = [Index("query", unique = true)])
data class SearchHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "query") val query: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
)
