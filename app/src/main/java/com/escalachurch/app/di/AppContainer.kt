package com.escalachurch.app.di

import android.content.Context
import com.escalachurch.app.data.local.AppDatabase
import com.escalachurch.app.data.preferences.SettingsDataStore
import com.escalachurch.app.data.preferences.UserProfileDataStore
import com.escalachurch.app.data.remote.SupabaseClientProvider
import com.escalachurch.app.data.repository.AnnouncementRepository
import com.escalachurch.app.data.repository.BulletinRepository
import com.escalachurch.app.data.repository.ChangeLogRepository
import com.escalachurch.app.data.repository.CustomEventRepository
import com.escalachurch.app.data.repository.DoxologyRepository
import com.escalachurch.app.data.repository.GeneralScaleRepository
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.data.repository.SonoplastiaFileRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.security.AdminSession

/**
 * Minimal, manual dependency container. No DI framework is required; this keeps the build
 * lighter while still separating construction from usage for testability.
 *
 * Official data (scales, doxologies, announcements, shared files) lives in Supabase (Postgres +
 * Auth + Storage) - see data/remote - so it's shared in real time by every device. Personal data
 * (Programar's CustomEvent, local settings, reminder state) stays in Room/DataStore on-device,
 * since it was never meant to be shared.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val userProfileDataStore = UserProfileDataStore(context)

    private val supabase = SupabaseClientProvider.client

    val scaleRepository = ScaleRepository(supabase)
    val doxologyRepository = DoxologyRepository(supabase)
    val customEventRepository = CustomEventRepository(database.customEventDao())
    val settingsRepository = SettingsRepository(settingsDataStore)
    val userProfileRepository = UserProfileRepository(userProfileDataStore)
    val announcementRepository = AnnouncementRepository(supabase)
    val bulletinRepository = BulletinRepository(supabase)
    val changeLogRepository = ChangeLogRepository(database.changeLogDao())
    val sonoplastiaFileRepository = SonoplastiaFileRepository(supabase)

    val generalScaleRepository = GeneralScaleRepository(
        context = context,
        scaleRepository = scaleRepository,
        changeLogRepository = changeLogRepository,
        settingsRepository = settingsRepository
    )

    val adminSession = AdminSession(supabase, userProfileRepository)
}
