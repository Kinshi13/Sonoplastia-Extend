package com.escalachurch.app.domain.util

import com.escalachurch.app.domain.model.VisualQuality

/** What decorative code actually renders with, after AUTOMATIC has been resolved against the
 *  device - screens should never branch on [VisualQuality] directly. */
enum class EffectiveVisualQuality { REDUCED, FULL }

enum class CanvasDetailLevel { SIMPLE, FULL }

/** Every signal [resolveEffectiveVisualSettings] needs - one bag of inputs so the function stays
 *  pure and every combination is trivially testable, instead of each caller reading Context/
 *  Lifecycle itself and re-deriving the precedence rules. See VisualEffectsController for how the
 *  UI layer gathers these (device RAM tier, battery saver, system "remove animations", lifecycle). */
data class VisualQualityInputs(
    val qualityPreference: VisualQuality,
    val parallaxPreference: Boolean,
    val animationsEnabled: Boolean,
    val systemReducedMotion: Boolean,
    val isLowRamDevice: Boolean,
    val isPowerSaveMode: Boolean,
    val isAppVisible: Boolean
)

/** The single resolved state every Celestial decorative component should consume - nothing
 *  downstream re-interprets [VisualQualityInputs] on its own (Bloco 14: "os componentes devem
 *  consumir esse estado central, e não interpretar as preferências separadamente"). */
data class EffectiveVisualSettings(
    val parallaxActive: Boolean,
    val ambientMotionActive: Boolean,
    val starDensity: Int,
    val glowIntensity: Float,
    val blurAllowed: Boolean,
    /** Multiplies normal animation durations; 0f means "skip the animation, jump to the end". */
    val animationDurationScale: Float,
    val canvasDetailLevel: CanvasDetailLevel
)

/**
 * Fase 11.9B Bloco 12/14 - the one place that combines quality preference, the dedicated parallax
 * toggle, animation preference, device/OS signals and app visibility into what components actually
 * render with. Precedence (Bloco 14 spec, in order):
 *   1. System reduced motion always wins - overrides FULL and any explicit toggle.
 *   2. animationsEnabled = false disables reactive/ambient parallax too.
 *   3. REDUCED quality caps effects even if the parallax toggle is on.
 *   4. FULL quality still can't bypass rule 1.
 *   5. AUTOMATIC picks a baseline (device RAM tier + battery saver) but user toggles (parallax/
 *      animations) still apply on top of it.
 *   6. App not visible (backgrounded) => no active motion, regardless of every other rule.
 *
 * Pure - see VisualQualityResolverTest.
 */
fun resolveEffectiveVisualSettings(inputs: VisualQualityInputs): EffectiveVisualSettings {
    val resolvedQuality = when (inputs.qualityPreference) {
        VisualQuality.FULL -> EffectiveVisualQuality.FULL
        VisualQuality.REDUCED -> EffectiveVisualQuality.REDUCED
        VisualQuality.AUTOMATIC ->
            if (inputs.isLowRamDevice || inputs.isPowerSaveMode) EffectiveVisualQuality.REDUCED else EffectiveVisualQuality.FULL
    }

    val motionAllowed = inputs.animationsEnabled && !inputs.systemReducedMotion
    val parallaxActive = inputs.parallaxPreference &&
        motionAllowed &&
        resolvedQuality == EffectiveVisualQuality.FULL &&
        inputs.isAppVisible

    val reduced = resolvedQuality == EffectiveVisualQuality.REDUCED
    return EffectiveVisualSettings(
        parallaxActive = parallaxActive,
        ambientMotionActive = parallaxActive,
        starDensity = if (reduced) 20 else 44,
        glowIntensity = if (reduced) 0.08f else 0.16f,
        blurAllowed = !reduced,
        animationDurationScale = if (!motionAllowed) 0f else if (reduced) 0.5f else 1f,
        canvasDetailLevel = if (reduced) CanvasDetailLevel.SIMPLE else CanvasDetailLevel.FULL
    )
}
