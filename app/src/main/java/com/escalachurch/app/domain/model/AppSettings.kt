package com.escalachurch.app.domain.model

/** User-configurable app-wide settings, persisted locally via DataStore. */
data class AppSettings(
    val fontSize: FontSizeOption = FontSizeOption.STANDARD,
    val selectedFont: AppFont = AppFont.SYSTEM_DEFAULT,
    val effectsVolume: Float = 0.7f,
    val animationsEnabled: Boolean = true,
    /** Fase 11.9B Bloco 12 - master switch for decorative-only effects (ambient parallax drift,
     *  halos): [animationsEnabled] already covers UI motion in general, this is specifically the
     *  "Efeitos visuais" toggle shown in Configurações. */
    val visualEffectsEnabled: Boolean = true,
    /** Fase 11.9B Bloco 12 - how much decorative detail (stars, blur, glow) to render; see
     *  [com.escalachurch.app.domain.util.resolveEffectiveVisualQuality] for how AUTOMATIC picks
     *  between REDUCED/FULL based on the device. */
    val visualQuality: VisualQuality = VisualQuality.AUTOMATIC,
    val vibrationEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    /** Local user profile name, used to match role fields (Recepção, Sonoplastia, etc.) for reminders. */
    val myName: String = "",
    val remindersEnabled: Boolean = true,
    val notifyDayBefore: Boolean = true,
    val notifyHoursBefore: Boolean = true,
    val reminderHoursBeforeLead: Int = 3,
    /** Notify when an official scale/announcement relevant to the user's classes changes. */
    val changeNotificationsEnabled: Boolean = true,
    /** true = only changes matching selectedClasses; false = every official change. */
    val notifyOnlyMyClasses: Boolean = true,
    /** Show the "Nova alteração" pop-up on app open when there are unseen relevant changes. */
    val showNewsPopupOnOpen: Boolean = true,
    /** Epoch millis of the last time the user opened Anúncios; drives the "Novo" badge/count. */
    val lastSeenAnnouncementsAt: Long = 0L
)
