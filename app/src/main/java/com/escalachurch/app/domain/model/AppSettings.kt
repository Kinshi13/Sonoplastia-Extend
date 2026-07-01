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
    val themeMode: ThemeMode = ThemeMode.AUTO,
    /** Local user profile name, used to match role fields (Recepção, Sonoplastia, etc.) for reminders. */
    val myName: String = "",
    val remindersEnabled: Boolean = true,
    val notifyDayBefore: Boolean = true,
    val notifyHoursBefore: Boolean = true,
    val reminderHoursBeforeLead: Int = 3,
    /** Standalone (default, fully local) vs connected to a shared church workspace once a backend exists. */
    val syncMode: SyncMode = SyncMode.STANDALONE,
    val workspaceName: String = ""
)
