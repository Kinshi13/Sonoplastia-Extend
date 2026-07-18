package com.escalachurch.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.escalachurch.app.domain.model.RecentChurch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.recentChurchDataStore by preferencesDataStore(name = "escala_church_recent_churches")

/**
 * Fase 11.9 Parte 3 - data-layer contract for "Continuar em" (ChurchEntryScreen is a later step).
 * Deliberately holds only what RecentChurch itself defines - no password, no admin token, so this
 * file is safe on its own even if the device is lost/backed up.
 */
class RecentChurchStore(private val context: Context) {

    private object Keys {
        val ENTRIES = stringPreferencesKey("recent_churches_json")
    }

    @Serializable
    private data class StoredEntry(
        @SerialName("church_id") val churchId: String,
        @SerialName("church_code") val churchCode: String,
        @SerialName("church_name") val churchName: String,
        @SerialName("logo_url") val logoUrl: String? = null,
        @SerialName("last_accessed_at") val lastAccessedAt: Long,
        @SerialName("is_favorite") val isFavorite: Boolean = false
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Favorite first, then most recently accessed - the same ordering the entry screen's list
     *  is expected to render in. */
    val recentChurchesFlow: Flow<List<RecentChurch>> = context.recentChurchDataStore.data.map { prefs ->
        val raw = prefs[Keys.ENTRIES] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<StoredEntry>>(raw) }.getOrNull()
            ?.map { RecentChurch(it.churchId, it.churchCode, it.churchName, it.logoUrl, it.lastAccessedAt, it.isFavorite) }
            ?.sortedWith(compareByDescending<RecentChurch> { it.isFavorite }.thenByDescending { it.lastAccessedAt })
            ?: emptyList()
    }

    /** Adds/updates one church's entry (bumping lastAccessedAt to now) and persists it. */
    suspend fun recordAccess(churchId: String, churchCode: String, churchName: String, logoUrl: String? = null) {
        val current = recentChurchesFlow.first()
        val existingFavorite = current.firstOrNull { it.churchId == churchId }?.isFavorite ?: false
        val next = current.filterNot { it.churchId == churchId } +
            RecentChurch(churchId, churchCode, churchName, logoUrl, System.currentTimeMillis(), existingFavorite)
        save(next)
    }

    suspend fun setFavorite(churchId: String, isFavorite: Boolean) {
        val next = recentChurchesFlow.first().map { if (it.churchId == churchId) it.copy(isFavorite = isFavorite) else it }
        save(next)
    }

    suspend fun remove(churchId: String) {
        val next = recentChurchesFlow.first().filterNot { it.churchId == churchId }
        save(next)
    }

    private suspend fun save(entries: List<RecentChurch>) {
        val stored = entries.map { StoredEntry(it.churchId, it.churchCode, it.churchName, it.logoUrl, it.lastAccessedAt, it.isFavorite) }
        context.recentChurchDataStore.edit { prefs -> prefs[Keys.ENTRIES] = json.encodeToString(stored) }
    }
}
