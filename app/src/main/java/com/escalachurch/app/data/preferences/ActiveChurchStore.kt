package com.escalachurch.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.escalachurch.app.domain.model.Church
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.activeChurchDataStore by preferencesDataStore(name = "escala_church_active_church")

/**
 * Fase 11.9A - persists which church this device is currently pointed at, so the app remembers it
 * across restarts and app updates (DataStore survives both - it's only cleared if the app itself
 * is uninstalled or its data is manually wiped; not included in Android Auto Backup - see
 * AndroidManifest's data_extraction_rules.xml, which only lists `database`/`sharedpref` domains,
 * not `file` - so a reinstall never silently restores an active church). See ActiveChurchManager
 * for the runtime bootstrap state built on top of this and LegacyChurchMigration.
 */
class ActiveChurchStore(private val context: Context) {

    private object Keys {
        val ID = stringPreferencesKey("active_church_id")
        val SLUG = stringPreferencesKey("active_church_slug")
        val NAME = stringPreferencesKey("active_church_name")
        // Hotfix: whether the one-time legacy-migration check has already run - distinct from
        // "is there an active church," so a genuinely new install that has no legacy marker only
        // ever gets evaluated once (and lands on NeedsChurchEntry), instead of re-running the
        // check (and logging noise) on every cold start.
        val MIGRATION_EVALUATED = booleanPreferencesKey("legacy_church_migration_evaluated")
    }

    val activeChurchFlow: Flow<Church?> = context.activeChurchDataStore.data.map { prefs ->
        val id = prefs[Keys.ID] ?: return@map null
        val slug = prefs[Keys.SLUG] ?: return@map null
        Church(id = id, slug = slug, name = prefs[Keys.NAME] ?: "", isActive = true)
    }

    val migrationEvaluatedFlow: Flow<Boolean> = context.activeChurchDataStore.data.map { it[Keys.MIGRATION_EVALUATED] ?: false }

    suspend fun isMigrationEvaluated(): Boolean = migrationEvaluatedFlow.first()

    suspend fun markMigrationEvaluated() {
        context.activeChurchDataStore.edit { prefs -> prefs[Keys.MIGRATION_EVALUATED] = true }
    }

    suspend fun save(church: Church) {
        context.activeChurchDataStore.edit { prefs ->
            prefs[Keys.ID] = church.id
            prefs[Keys.SLUG] = church.slug
            prefs[Keys.NAME] = church.name
        }
    }
}
