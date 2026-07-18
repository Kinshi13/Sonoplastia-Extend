package com.escalachurch.app.domain.model

/** User-configurable app-wide settings, persisted locally via DataStore. */
data class AppSettings(
    val fontSize: FontSizeOption = FontSizeOption.STANDARD,
    val selectedFont: AppFont = AppFont.SYSTEM_DEFAULT,
    val effectsVolume: Float = 0.7f,
    val animationsEnabled: Boolean = true,
    /** Fase 11.9B Bloco 14 - dedicated "Parallax: Ativado/Desativado" control (Bloco 12 first
     *  wired this to the field then named "visualEffectsEnabled"; renamed here to say what it
     *  actually does). [animationsEnabled] still covers UI motion in general - this is only the
     *  reactive/ambient parallax layer. See VisualQualityResolver for how this combines with
     *  [visualQuality], device capability and system accessibility settings. */
    val parallaxEnabled: Boolean = true,
    /** Fase 11.9B Bloco 12/14 - how much decorative detail (stars, glow, blur) to render; see
     *  [com.escalachurch.app.domain.util.resolveEffectiveVisualSettings] for how AUTOMATIC picks
     *  between REDUCED/FULL based on the device and battery state. */
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
