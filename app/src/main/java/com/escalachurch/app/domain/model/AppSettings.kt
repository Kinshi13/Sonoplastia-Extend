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
    val workspaceName: String = "",
    /** Notify when an official scale/announcement relevant to the user's classes changes. */
    val changeNotificationsEnabled: Boolean = true,
    /** true = only changes matching selectedClasses; false = every official change. */
    val notifyOnlyMyClasses: Boolean = true,
    /** Show the "Nova alteração" pop-up on app open when there are unseen relevant changes. */
    val showNewsPopupOnOpen: Boolean = true,
    /**
     * SHA-256 hex hash of the local admin PIN, or null if no PIN was set yet.
     * TODO(auth): temporary local-only gate; replace with real authentication
     * (Firebase Auth / Supabase Auth / API própria) issuing a verified admin role.
     */
    val adminPinHash: String? = null,
    /** Epoch millis of the last time the user opened Anúncios; drives the "Novo" badge/count. */
    val lastSeenAnnouncementsAt: Long = 0L
)
