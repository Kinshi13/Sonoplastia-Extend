package com.escalachurch.app.di

import android.content.Context
import com.escalachurch.app.data.local.AppDatabase
import com.escalachurch.app.data.preferences.SettingsDataStore
import com.escalachurch.app.data.preferences.UserProfileDataStore
import com.escalachurch.app.data.repository.AnnouncementRepository
import com.escalachurch.app.data.repository.ChangeLogRepository
import com.escalachurch.app.data.repository.CustomEventRepository
import com.escalachurch.app.data.repository.DoxologyRepository
import com.escalachurch.app.data.repository.GeneralScaleRepository
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.data.sync.LocalOnlySyncGateway
import com.escalachurch.app.data.sync.ScheduleSyncGateway
import com.escalachurch.app.security.AdminSession

/**
 * Minimal, manual dependency container. No DI framework is required since the
 * app is fully local/offline; this keeps the build lighter while still
 * separating construction from usage for testability.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val userProfileDataStore = UserProfileDataStore(context)

    val scaleRepository = ScaleRepository(database.scaleDao())
    val doxologyRepository = DoxologyRepository(database.doxologyDao())
    val customEventRepository = CustomEventRepository(database.customEventDao())
    val settingsRepository = SettingsRepository(settingsDataStore)
    val userProfileRepository = UserProfileRepository(userProfileDataStore)
    val announcementRepository = AnnouncementRepository(database.announcementDao())
    val changeLogRepository = ChangeLogRepository(database.changeLogDao())

    val generalScaleRepository = GeneralScaleRepository(
        context = context,
        scaleRepository = scaleRepository,
        changeLogRepository = changeLogRepository,
        settingsRepository = settingsRepository
    )

    val adminSession = AdminSession(settingsRepository, userProfileRepository)

    /** Swap for a Firebase-backed implementation once a backend project exists; no other code changes. */
    val scheduleSyncGateway: ScheduleSyncGateway = LocalOnlySyncGateway()
}
