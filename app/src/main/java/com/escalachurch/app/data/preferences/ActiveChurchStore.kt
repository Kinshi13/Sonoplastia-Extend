package com.escalachurch.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.escalachurch.app.domain.model.Church
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.activeChurchDataStore by preferencesDataStore(name = "escala_church_active_church")

/**
 * Fase 11.9A - persists which church this device is currently pointed at, so the app remembers it
 * across restarts and app updates (DataStore survives both - it's only cleared if the app itself
 * is uninstalled or its data is manually wiped). See ActiveChurchManager for the runtime state
 * built on top of this and the BuildConfig.CHURCH_ID bootstrap-migration fallback.
 */
class ActiveChurchStore(private val context: Context) {

    private object Keys {
        val ID = stringPreferencesKey("active_church_id")
        val SLUG = stringPreferencesKey("active_church_slug")
        val NAME = stringPreferencesKey("active_church_name")
    }

    val activeChurchFlow: Flow<Church?> = context.activeChurchDataStore.data.map { prefs ->
        val id = prefs[Keys.ID] ?: return@map null
        val slug = prefs[Keys.SLUG] ?: return@map null
        Church(id = id, slug = slug, name = prefs[Keys.NAME] ?: "", isActive = true)
    }

    suspend fun save(church: Church) {
        context.activeChurchDataStore.edit { prefs ->
            prefs[Keys.ID] = church.id
            prefs[Keys.SLUG] = church.slug
            prefs[Keys.NAME] = church.name
        }
    }
}
