package com.escalachurch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.escalachurch.app.data.local.entity.ChangeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChangeLogDao {

    @Query("SELECT * FROM change_log ORDER BY changedAt DESC")
    fun observeAll(): Flow<List<ChangeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChangeLogEntity): Long

    @Update
    suspend fun update(entity: ChangeLogEntity)
}
