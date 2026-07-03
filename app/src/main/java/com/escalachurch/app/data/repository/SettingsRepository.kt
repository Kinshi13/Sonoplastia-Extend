package com.escalachurch.app.data.repository

import com.escalachurch.app.data.preferences.SettingsDataStore
import com.escalachurch.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dataStore: SettingsDataStore) {

    val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow

    suspend fun update(settings: AppSettings) = dataStore.update(settings)

    val notifiedReminderKeysFlow: Flow<Set<String>> = dataStore.notifiedKeysFlow

    suspend fun markReminderNotified(key: String) = dataStore.markNotified(key)

    suspend fun pruneReminderKeys(validScaleIds: Set<String>) = dataStore.pruneNotifiedKeys(validScaleIds)
}
