package com.escalachurch.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.escalachurch.app.domain.model.AppFont
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.model.FontSizeOption
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
                ?: ThemeMode.AUTO
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
        }
    }
}
