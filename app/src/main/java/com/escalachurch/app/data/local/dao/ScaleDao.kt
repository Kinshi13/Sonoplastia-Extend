package com.escalachurch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.escalachurch.app.data.local.entity.ScaleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ScaleDao {

    @Query("SELECT * FROM scales ORDER BY date ASC, startTime ASC")
    fun observeAll(): Flow<List<ScaleEntity>>

    @Query("SELECT * FROM scales WHERE date = :date ORDER BY startTime ASC")
    suspend fun getByDate(date: LocalDate): List<ScaleEntity>

    @Query("SELECT * FROM scales WHERE id = :id")
    suspend fun getById(id: Long): ScaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScaleEntity): Long

    @Update
    suspend fun update(entity: ScaleEntity)

    @Delete
    suspend fun delete(entity: ScaleEntity)

    @Query("DELETE FROM scales WHERE id = :id")
    suspend fun deleteById(id: Long)
}
