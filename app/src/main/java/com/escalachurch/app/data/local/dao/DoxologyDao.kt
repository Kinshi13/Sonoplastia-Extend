package com.escalachurch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.escalachurch.app.data.local.entity.DoxologyEntity
import com.escalachurch.app.data.local.entity.ProgramStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoxologyDao {

    @Query("SELECT * FROM doxologies ORDER BY date ASC, startTime ASC")
    fun observeAll(): Flow<List<DoxologyEntity>>

    @Query("SELECT * FROM program_steps WHERE doxologyId = :doxologyId ORDER BY `order` ASC")
    fun observeSteps(doxologyId: Long): Flow<List<ProgramStepEntity>>

    @Query("SELECT * FROM program_steps ORDER BY `order` ASC")
    fun observeAllSteps(): Flow<List<ProgramStepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DoxologyEntity): Long

    @Update
    suspend fun update(entity: DoxologyEntity)

    @Delete
    suspend fun delete(entity: DoxologyEntity)

    @Query("DELETE FROM doxologies WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<ProgramStepEntity>)

    @Query("DELETE FROM program_steps WHERE doxologyId = :doxologyId")
    suspend fun deleteStepsForDoxology(doxologyId: Long)
}
