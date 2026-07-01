package com.escalachurch.app.di

import android.content.Context
import com.escalachurch.app.data.local.AppDatabase
import com.escalachurch.app.data.preferences.SettingsDataStore
import com.escalachurch.app.data.repository.CustomEventRepository
import com.escalachurch.app.data.repository.DoxologyRepository
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.data.sync.LocalOnlySyncGateway
import com.escalachurch.app.data.sync.ScheduleSyncGateway

/**
 * Minimal, manual dependency container. No DI framework is required since the
 * app is fully local/offline; this keeps the build lighter while still
 * separating construction from usage for testability.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val settingsDataStore = SettingsDataStore(context)

    val scaleRepository = ScaleRepository(database.scaleDao())
    val doxologyRepository = DoxologyRepository(database.doxologyDao())
    val customEventRepository = CustomEventRepository(database.customEventDao())
    val settingsRepository = SettingsRepository(settingsDataStore)

    /** Swap for a Firebase-backed implementation once a backend project exists; no other code changes. */
    val scheduleSyncGateway: ScheduleSyncGateway = LocalOnlySyncGateway()
}
