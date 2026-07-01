package com.escalachurch.app.domain.model

/** User-configurable app-wide settings, persisted locally via DataStore. */
data class AppSettings(
    val fontSize: FontSizeOption = FontSizeOption.STANDARD,
    val selectedFont: AppFont = AppFont.SYSTEM_DEFAULT,
    val musicVolume: Float = 0.7f,
    val effectsVolume: Float = 0.7f,
    val animationsEnabled: Boolean = true,
    val visualEffectsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.AUTO
)
