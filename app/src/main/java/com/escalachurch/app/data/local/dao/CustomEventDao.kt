package com.escalachurch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.escalachurch.app.data.local.entity.CustomEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEventDao {

    @Query("SELECT * FROM custom_events ORDER BY date ASC, startTime ASC")
    fun observeAll(): Flow<List<CustomEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CustomEventEntity>): List<Long>

    @Update
    suspend fun update(entity: CustomEventEntity)

    @Delete
    suspend fun delete(entity: CustomEventEntity)

    @Query("DELETE FROM custom_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM custom_events WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)
}
