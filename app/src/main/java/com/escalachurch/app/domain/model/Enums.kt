package com.escalachurch.app.domain.model

/** Type of a schedule/program card, used across Scale, Doxology and CustomEvent. */
enum class ProgramType(val label: String) {
    COMMON_SCALE("Escala comum"),
    DOXOLOGY("Doxologia"),
    SPECIAL_EVENT("Evento especial"),
    PRAYER_WEEK("Semana de Oração"),
    OTHER("Outro")
}

enum class FontSizeOption(val label: String, val scale: Float) {
    SMALL("Pequena", 0.88f),
    STANDARD("Padrão", 1.0f),
    LARGE("Grande", 1.15f),
    EXTRA_LARGE("Muito grande", 1.3f)
}

enum class ThemeMode(val label: String) {
    LIGHT("Claro"),
    DARK("Escuro"),
    AUTO("Automático")
}

/** Fase 11.9B Bloco 12 - user's preferred decorative-detail level (parallax drift, halo/glow,
 *  star count). AUTOMATIC lets the device decide (see resolveEffectiveVisualSettings) instead of
 *  forcing every phone into the same setting. */
enum class VisualQuality(val label: String) {
    AUTOMATIC("Automática"),
    REDUCED("Reduzida"),
    FULL("Completa")
}

enum class AppFont(val label: String, val isPremium: Boolean) {
    SYSTEM_DEFAULT("Padrão do sistema", false),
    SERIF_CLASSIC("Serifada clássica", false),
    ROUNDED("Arredondada", false),
    ELEGANT_SCRIPT("Elegante (em breve)", true),
    MODERN_PREMIUM("Moderna Premium (em breve)", true)
}

/** Simple weekly recurrence marker for custom events/programs. */
enum class RepeatRule {
    NONE,
    WEEKLY
}
