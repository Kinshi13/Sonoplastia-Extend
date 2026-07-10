package com.escalachurch.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * "Constellation Calm" - Fase 4 of the Master Plan. A design token layer for the astral/stellar,
 * minimalist-premium visual identity the product is moving toward (explicitly NOT gamey, not
 * neon, not corporate-flat). This file only DEFINES the tokens; no existing screen reads from it
 * yet - screens keep using [LightColors]/[DarkColors] from Theme.kt until the Android redesign
 * phase adopts these deliberately, one screen at a time. Additive on purpose, so Fase 4 can ship
 * with zero visual regression risk.
 *
 * Naming follows the night-sky metaphor everywhere, so a designer/dev can reason about roles
 * without memorizing hex codes:
 * - void / nebula(-Elevated): background surfaces, darkest to lightest
 * - starlight / stardust: primary/secondary text and iconography
 * - horizon: dividers, outlines, the faint line between surfaces
 * - polaris(-Soft): the calm, confident primary accent (interactive, focus, the "north star")
 * - aurora: a rarer secondary accent, used sparingly for highlights - never as a base color
 * - comet: gold, reserved for premium/Founder moments (reuses the existing SpecialGold hue)
 * - nova: error/danger
 */
object ConstellationColors {

    object Dark {
        val void = Color(0xFF090B14)
        val nebula = Color(0xFF12162A)
        val nebulaElevated = Color(0xFF1A1F3B)
        val starlight = Color(0xFFF3F4FA)
        val stardust = Color(0xFFA6ABC7)
        val horizon = Color(0xFF2A2F52)
        val polaris = Color(0xFF7C9CFF)
        val polarisSoft = Color(0xFF33396E)
        val aurora = Color(0xFF9C7CFF)
        val comet = SpecialGold
        val nova = Color(0xFFE5657A)
    }

    /** Same roles, tuned for a bright surface - "daylight constellation": the sky is pale, the
     *  stars still read as considered accents rather than a simple color inversion. */
    object Light {
        val void = Color(0xFFF6F7FC)
        val nebula = Color(0xFFFFFFFF)
        val nebulaElevated = Color(0xFFEFF1FA)
        val starlight = Color(0xFF171B2E)
        val stardust = Color(0xFF5C6280)
        val horizon = Color(0xFFDEE1F1)
        val polaris = Color(0xFF4A5FE0)
        val polarisSoft = Color(0xFFE1E5FC)
        val aurora = Color(0xFF7C5CE0)
        val comet = SpecialGold
        val nova = Color(0xFFD0324B)
    }
}

/** 4pt grid - every layout gap/padding in the redesign should resolve to one of these instead of
 *  an arbitrary dp value, so density stays consistent across screens. */
object ConstellationSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Calm, unhurried motion - deliberately no overshoot/bounce (that reads as "gamey", which the
 *  Master Plan rules out). One shared easing curve so every transition feels like part of the
 *  same system. */
object ConstellationMotion {
    const val QUICK_MS = 160
    const val STANDARD_MS = 280
    const val SLOW_MS = 420
    val stellarEase: Easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)
}

/** Soft, diffuse "glow" instead of a hard drop shadow - low opacity, tinted with [ConstellationColors.Dark.polaris]/
 *  [ConstellationColors.Light.polaris] rather than pure black, evoking starlight rather than a
 *  material paper shadow. Consumed by redesigned components once the Android redesign phase
 *  begins; values are pre-defined here so that phase can start from an agreed system. */
object ConstellationElevation {
    val restingDp = 0.dp
    val raisedDp = 2.dp
    val floatingDp = 8.dp
    const val GLOW_ALPHA = 0.18f
}
