package com.escalachurch.app.di

import android.content.Context
import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.church.LegacyChurchMigration
import com.escalachurch.app.data.local.AppDatabase
import com.escalachurch.app.data.preferences.ActiveChurchStore
import com.escalachurch.app.data.preferences.AnnouncementSpotlightStore
import com.escalachurch.app.data.preferences.RecentChurchStore
import com.escalachurch.app.data.preferences.SettingsDataStore
import com.escalachurch.app.data.preferences.UserProfileDataStore
import com.escalachurch.app.data.remote.SupabaseClientProvider
import com.escalachurch.app.data.repository.AnnouncementRepository
import com.escalachurch.app.data.repository.BulletinRepository
import com.escalachurch.app.data.repository.ChangeLogRepository
import com.escalachurch.app.data.repository.ChurchRepository
import com.escalachurch.app.data.repository.CustomEventRepository
import com.escalachurch.app.data.repository.DoxologyRepository
import com.escalachurch.app.data.repository.GeneralScaleRepository
import com.escalachurch.app.data.repository.PlanRepository
import com.escalachurch.app.data.repository.ProfileRepository
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.data.repository.SonoplastiaFileRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.entitlements.EntitlementCacheStore
import com.escalachurch.app.entitlements.EntitlementService
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    // Lives for as long as the container (effectively the process) - the active church and
    // entitlements need to keep resolving in the background regardless of which screen is on top.
    private val containerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Fase 11.9A: single source of truth for "which church is this session pointed at" - every
    // repository below that used to read BuildConfig.CHURCH_ID now reacts to this instead.
    // LegacyChurchMigration needs entitlementCacheStore/profileRepository built first (evidence
    // sources for the one-time migration check - see ActiveChurchManager/LegacyChurchMigration).
    val churchRepository = ChurchRepository(supabase)
    val recentChurchStore = RecentChurchStore(context)
    private val activeChurchStore = ActiveChurchStore(context)
    val entitlementCacheStore = EntitlementCacheStore(context)
    private val profileRepository = ProfileRepository(supabase)
    private val legacyChurchMigration = LegacyChurchMigration(context, supabase, profileRepository, entitlementCacheStore)
    val activeChurchManager = ActiveChurchManager(
        activeChurchStore, churchRepository, recentChurchStore, legacyChurchMigration, containerScope
    )

    val scaleRepository = ScaleRepository(supabase, activeChurchManager)
    val doxologyRepository = DoxologyRepository(supabase, activeChurchManager)
    val customEventRepository = CustomEventRepository(database.customEventDao())
    val settingsRepository = SettingsRepository(settingsDataStore)
    val userProfileRepository = UserProfileRepository(userProfileDataStore)
    val announcementRepository = AnnouncementRepository(supabase, activeChurchManager)
    val announcementSpotlightStore = AnnouncementSpotlightStore(context)
    // Fase 11.9B Bloco 2: last two repositories still pinned to BuildConfig.CHURCH_ID, now fixed.
    val bulletinRepository = BulletinRepository(supabase, activeChurchManager)
    val changeLogRepository = ChangeLogRepository(database.changeLogDao())
    val sonoplastiaFileRepository = SonoplastiaFileRepository(supabase, activeChurchManager)

    val planRepository = PlanRepository(supabase, activeChurchManager)
    val entitlementService = EntitlementService(planRepository, entitlementCacheStore, activeChurchManager, containerScope)

    val generalScaleRepository = GeneralScaleRepository(
        context = context,
        scaleRepository = scaleRepository,
        changeLogRepository = changeLogRepository,
        settingsRepository = settingsRepository
    )

    val adminSession = AdminSession(supabase, userProfileRepository, planRepository, profileRepository, churchRepository, activeChurchManager)
}
