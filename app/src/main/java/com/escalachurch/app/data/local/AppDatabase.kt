package com.escalachurch.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.escalachurch.app.data.local.converter.Converters
import com.escalachurch.app.data.local.dao.CustomEventDao
import com.escalachurch.app.data.local.dao.DoxologyDao
import com.escalachurch.app.data.local.dao.ScaleDao
import com.escalachurch.app.data.local.entity.CustomEventEntity
import com.escalachurch.app.data.local.entity.DoxologyEntity
import com.escalachurch.app.data.local.entity.ProgramStepEntity
import com.escalachurch.app.data.local.entity.ScaleEntity

@Database(
    entities = [
        ScaleEntity::class,
        DoxologyEntity::class,
        ProgramStepEntity::class,
        CustomEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scaleDao(): ScaleDao
    abstract fun doxologyDao(): DoxologyDao
    abstract fun customEventDao(): CustomEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "escala_church.db"
                ).build().also { INSTANCE = it }
            }
    }
}
