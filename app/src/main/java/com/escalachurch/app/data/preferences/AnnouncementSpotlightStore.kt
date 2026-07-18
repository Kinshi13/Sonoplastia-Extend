package com.escalachurch.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.spotlightDataStore by preferencesDataStore(name = "escala_church_announcement_spotlight")

/**
 * Fase 11.9B Entrega 3 Bloco 4 - "confirmed" tracking for AnnouncementSpotlight.
 *
 * There is no `announcement_views` table in the backend (checked schema.sql and
 * web/lib/types/database.ts - neither Android nor the web site has per-user/per-device view
 * tracking for announcements today, only the existing `settings.lastSeenAnnouncementsAt` single
 * timestamp used for the "Novo" badge). Per this phase's explicit instruction ("se não existir,
 * documentar a ausência e usar somente um cache local temporário sem criar schema incompatível"),
 * this is exactly that: a local-only, best-effort cache - not a synced view log, not visible to
 * admins, lost if the app is reinstalled. A real cross-device `announcement_views` table is a
 * backend feature for a later phase, not invented here.
 *
 * Keyed by church_id so confirming an announcement in one church never affects another's Spotlight
 * (Bloco 23: "não misturar visualizações entre igrejas").
 */
class AnnouncementSpotlightStore(private val context: Context) {

    @Serializable
    private data class ConfirmedEntry(val announcementId: String, val confirmedAtUpdatedAt: Long)

    private fun key(churchId: String) = stringPreferencesKey("confirmed_$churchId")

    private val json = Json { ignoreUnknownKeys = true }

    /** Announcement ids this church has already confirmed, each paired with the announcement's
     *  `updatedAt` *at the time of confirmation* - if the real announcement's updatedAt moves past
     *  this value, it counts as "changed since confirmation" and should resurface (Bloco 4: "se
     *  não... alteração foi irrelevante" vs a relevant one). */
    suspend fun confirmedUpdatedAt(churchId: String): Map<String, Long> {
        val raw = context.spotlightDataStore.data.map { it[key(churchId)] }.first() ?: return emptyMap()
        return runCatching { json.decodeFromString<List<ConfirmedEntry>>(raw) }.getOrNull()
            ?.associate { it.announcementId to it.confirmedAtUpdatedAt }
            ?: emptyMap()
    }

    suspend fun markConfirmed(churchId: String, announcementId: String, updatedAt: Long) {
        val current = confirmedUpdatedAt(churchId).toMutableMap()
        current[announcementId] = updatedAt
        save(churchId, current)
    }

    private suspend fun save(churchId: String, entries: Map<String, Long>) {
        val list = entries.map { (id, updatedAt) -> ConfirmedEntry(id, updatedAt) }
        context.spotlightDataStore.edit { prefs -> prefs[key(churchId)] = json.encodeToString(list) }
    }
}
