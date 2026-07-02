package com.escalachurch.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.escalachurch.app.data.local.converter.Converters
import com.escalachurch.app.data.local.dao.AnnouncementDao
import com.escalachurch.app.data.local.dao.ChangeLogDao
import com.escalachurch.app.data.local.dao.CustomEventDao
import com.escalachurch.app.data.local.dao.DoxologyDao
import com.escalachurch.app.data.local.dao.ScaleDao
import com.escalachurch.app.data.local.entity.AnnouncementEntity
import com.escalachurch.app.data.local.entity.ChangeLogEntity
import com.escalachurch.app.data.local.entity.CustomEventEntity
import com.escalachurch.app.data.local.entity.DoxologyEntity
import com.escalachurch.app.data.local.entity.ProgramStepEntity
import com.escalachurch.app.data.local.entity.ScaleEntity

@Database(
    entities = [
        ScaleEntity::class,
        DoxologyEntity::class,
        ProgramStepEntity::class,
        CustomEventEntity::class,
        AnnouncementEntity::class,
        ChangeLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scaleDao(): ScaleDao
    abstract fun doxologyDao(): DoxologyDao
    abstract fun customEventDao(): CustomEventDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun changeLogDao(): ChangeLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "escala_church.db"
                )
                    // MVP local-only app, no production installs to preserve yet - destructive
                    // migration is acceptable here; switch to real Migration objects once the
                    // app has real users with data worth keeping across schema changes.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
