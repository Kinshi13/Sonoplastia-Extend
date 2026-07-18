package com.escalachurch.app.church

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.escalachurch.app.BuildConfig
import com.escalachurch.app.data.repository.ProfileRepository
import com.escalachurch.app.entitlements.EntitlementCacheStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth

/** What justified adopting a church without the user typing a code - only ever surfaced in debug
 *  logs (see ActiveChurchManager), never persisted or shown to the user. */
sealed class LegacyMarker {
    /** Strongest signal: an already-authenticated Supabase session resolved a real church_id via
     *  the account's own `profiles` row - not a guess, server-verified. Never touches BuildConfig. */
    data class AuthenticatedProfile(val churchId: String) : LegacyMarker()

    /** A DataStore file that predates Fase 11.9A exists on disk - only possible on a device that
     *  genuinely ran an earlier version in place (these files are NOT included in Android Auto
     *  Backup, see ActiveChurchStore's doc, so a reinstall can never fake this). BuildConfig.
     *  CHURCH_ID is the only available church_id for this case (that's what the earlier version
     *  was pinned to). */
    data object LegacyLocalData : LegacyMarker()

    data object None : LegacyMarker()
}

/**
 * Fase 11.9A hotfix - the original bootstrap treated "ActiveChurchStore is empty" as proof of a
 * legacy (pre-11.9A) install and auto-adopted BuildConfig.CHURCH_ID. That's wrong: a brand-new
 * install's ActiveChurchStore is equally empty. This class requires actual evidence instead -
 * see findMarker(). If none of these is present, BuildConfig.CHURCH_ID is never consulted and the
 * install is treated as new (ChurchEntryScreen).
 */
class LegacyChurchMigration(
    private val context: Context,
    private val client: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val entitlementCacheStore: EntitlementCacheStore
) {
    suspend fun findMarker(): LegacyMarker {
        client.auth.currentUserOrNull()?.id?.let { userId ->
            profileRepository.fetchChurchId(userId)?.let { return LegacyMarker.AuthenticatedProfile(it) }
        }
        val hasLegacyPreferences = context.preferencesDataStoreFile("escala_church_user_profile").exists() ||
            context.preferencesDataStoreFile("escala_church_settings").exists()
        val hasCachedEntitlement = entitlementCacheStore.current() != null
        return if (hasLegacyPreferences || hasCachedEntitlement) LegacyMarker.LegacyLocalData else LegacyMarker.None
    }

    /** Only meaningful for [LegacyMarker.LegacyLocalData] - the pre-11.9A single-tenant build had
     *  no other way to know its church. Blank means this build was never configured with one
     *  either (e.g. a dev build), so there is nothing to migrate to. */
    fun legacyChurchId(): String? = BuildConfig.CHURCH_ID.takeIf { it.isNotBlank() }
}
