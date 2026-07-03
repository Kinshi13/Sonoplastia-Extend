package com.escalachurch.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.escalachurch.app.data.local.converter.Converters
import com.escalachurch.app.data.local.dao.ChangeLogDao
import com.escalachurch.app.data.local.dao.CustomEventDao
import com.escalachurch.app.data.local.entity.ChangeLogEntity
import com.escalachurch.app.data.local.entity.CustomEventEntity

/**
 * Local-only data: personal programações (Programar) and the on-device change-log cache. Official
 * data (scales, doxologies, announcements) now lives in Firebase Firestore instead - see
 * FirestoreCollections and AppContainer.
 */
@Database(
    entities = [
        CustomEventEntity::class,
        ChangeLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customEventDao(): CustomEventDao
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
