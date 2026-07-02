package com.escalachurch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.escalachurch.app.data.local.entity.AnnouncementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnouncementDao {

    @Query("SELECT * FROM announcements WHERE isActive = 1 ORDER BY isPinned DESC, publishedAt DESC")
    fun observeActive(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AnnouncementEntity): Long

    @Update
    suspend fun update(entity: AnnouncementEntity)

    @Delete
    suspend fun delete(entity: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteById(id: Long)
}
