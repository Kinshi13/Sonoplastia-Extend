package com.escalachurch.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.escalachurch.app.domain.model.AppFont
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.model.FontSizeOption
import com.escalachurch.app.domain.model.SyncMode
import com.escalachurch.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "escala_church_settings")

/** Local, no-server persistence for [AppSettings] using Jetpack DataStore. */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val FONT_SIZE = stringPreferencesKey("font_size")
        val SELECTED_FONT = stringPreferencesKey("selected_font")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val EFFECTS_VOLUME = floatPreferencesKey("effects_volume")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val VISUAL_EFFECTS_ENABLED = booleanPreferencesKey("visual_effects_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MY_NAME = stringPreferencesKey("my_name")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val NOTIFY_DAY_BEFORE = booleanPreferencesKey("notify_day_before")
        val NOTIFY_HOURS_BEFORE = booleanPreferencesKey("notify_hours_before")
        val REMINDER_HOURS_LEAD = intPreferencesKey("reminder_hours_lead")
        val NOTIFIED_KEYS = stringSetPreferencesKey("notified_reminder_keys")
        val SYNC_MODE = stringPreferencesKey("sync_mode")
        val WORKSPACE_NAME = stringPreferencesKey("workspace_name")
        val CHANGE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("change_notifications_enabled")
        val NOTIFY_ONLY_MY_CLASSES = booleanPreferencesKey("notify_only_my_classes")
        val SHOW_NEWS_POPUP_ON_OPEN = booleanPreferencesKey("show_news_popup_on_open")
        val ADMIN_PIN_HASH = stringPreferencesKey("admin_pin_hash")
        val LAST_SEEN_ANNOUNCEMENTS_AT = androidx.datastore.preferences.core.longPreferencesKey("last_seen_announcements_at")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            fontSize = prefs[Keys.FONT_SIZE]?.let { runCatching { FontSizeOption.valueOf(it) }.getOrNull() }
                ?: FontSizeOption.STANDARD,
            selectedFont = prefs[Keys.SELECTED_FONT]?.let { runCatching { AppFont.valueOf(it) }.getOrNull() }
                ?: AppFont.SYSTEM_DEFAULT,
            musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.7f,
            effectsVolume = prefs[Keys.EFFECTS_VOLUME] ?: 0.7f,
            animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
            visualEffectsEnabled = prefs[Keys.VISUAL_EFFECTS_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.AUTO,
            myName = prefs[Keys.MY_NAME] ?: "",
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
            notifyDayBefore = prefs[Keys.NOTIFY_DAY_BEFORE] ?: true,
            notifyHoursBefore = prefs[Keys.NOTIFY_HOURS_BEFORE] ?: true,
            reminderHoursBeforeLead = prefs[Keys.REMINDER_HOURS_LEAD] ?: 3,
            syncMode = prefs[Keys.SYNC_MODE]?.let { runCatching { SyncMode.valueOf(it) }.getOrNull() }
                ?: SyncMode.STANDALONE,
            workspaceName = prefs[Keys.WORKSPACE_NAME] ?: "",
            changeNotificationsEnabled = prefs[Keys.CHANGE_NOTIFICATIONS_ENABLED] ?: true,
            notifyOnlyMyClasses = prefs[Keys.NOTIFY_ONLY_MY_CLASSES] ?: true,
            showNewsPopupOnOpen = prefs[Keys.SHOW_NEWS_POPUP_ON_OPEN] ?: true,
            adminPinHash = prefs[Keys.ADMIN_PIN_HASH],
            lastSeenAnnouncementsAt = prefs[Keys.LAST_SEEN_ANNOUNCEMENTS_AT] ?: 0L
        )
    }

    suspend fun update(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_SIZE] = settings.fontSize.name
            prefs[Keys.SELECTED_FONT] = settings.selectedFont.name
            prefs[Keys.MUSIC_VOLUME] = settings.musicVolume
            prefs[Keys.EFFECTS_VOLUME] = settings.effectsVolume
            prefs[Keys.ANIMATIONS_ENABLED] = settings.animationsEnabled
            prefs[Keys.VISUAL_EFFECTS_ENABLED] = settings.visualEffectsEnabled
            prefs[Keys.VIBRATION_ENABLED] = settings.vibrationEnabled
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.MY_NAME] = settings.myName
            prefs[Keys.REMINDERS_ENABLED] = settings.remindersEnabled
            prefs[Keys.NOTIFY_DAY_BEFORE] = settings.notifyDayBefore
            prefs[Keys.NOTIFY_HOURS_BEFORE] = settings.notifyHoursBefore
            prefs[Keys.REMINDER_HOURS_LEAD] = settings.reminderHoursBeforeLead
            prefs[Keys.SYNC_MODE] = settings.syncMode.name
            prefs[Keys.WORKSPACE_NAME] = settings.workspaceName
            prefs[Keys.CHANGE_NOTIFICATIONS_ENABLED] = settings.changeNotificationsEnabled
            prefs[Keys.NOTIFY_ONLY_MY_CLASSES] = settings.notifyOnlyMyClasses
            prefs[Keys.SHOW_NEWS_POPUP_ON_OPEN] = settings.showNewsPopupOnOpen
            if (settings.adminPinHash != null) {
                prefs[Keys.ADMIN_PIN_HASH] = settings.adminPinHash
            } else {
                prefs.remove(Keys.ADMIN_PIN_HASH)
            }
            prefs[Keys.LAST_SEEN_ANNOUNCEMENTS_AT] = settings.lastSeenAnnouncementsAt
        }
    }

    /** Keys already notified (e.g. "scaleId:DAY_BEFORE"), so the periodic worker never repeats a reminder. */
    val notifiedKeysFlow: Flow<Set<String>> = context.dataStore.data.map { it[Keys.NOTIFIED_KEYS] ?: emptySet() }

    suspend fun markNotified(key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFIED_KEYS] = (prefs[Keys.NOTIFIED_KEYS] ?: emptySet()) + key
        }
    }

    /** Drops notified keys for reminder kinds no longer relevant, keeping the stored set from growing forever. */
    suspend fun pruneNotifiedKeys(validScaleIds: Set<Long>) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.NOTIFIED_KEYS] ?: emptySet()
            prefs[Keys.NOTIFIED_KEYS] = current.filter { key ->
                key.substringBefore(':').toLongOrNull() in validScaleIds
            }.toSet()
        }
    }
}
