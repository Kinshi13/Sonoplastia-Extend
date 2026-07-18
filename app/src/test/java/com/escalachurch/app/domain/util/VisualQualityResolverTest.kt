package com.escalachurch.app.domain.util

import com.escalachurch.app.domain.model.VisualQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualQualityResolverTest {

    private fun inputs(
        quality: VisualQuality = VisualQuality.FULL,
        parallax: Boolean = true,
        animations: Boolean = true,
        systemReducedMotion: Boolean = false,
        lowRam: Boolean = false,
        powerSave: Boolean = false,
        visible: Boolean = true
    ) = VisualQualityInputs(
        qualityPreference = quality,
        parallaxPreference = parallax,
        animationsEnabled = animations,
        systemReducedMotion = systemReducedMotion,
        isLowRamDevice = lowRam,
        isPowerSaveMode = powerSave,
        isAppVisible = visible
    )

    @Test
    fun full_withEverythingOn_activatesParallaxAndFullDetail() {
        val result = resolveEffectiveVisualSettings(inputs(quality = VisualQuality.FULL))
        assertTrue(result.parallaxActive)
        assertTrue(result.ambientMotionActive)
        assertEquals(44, result.starDensity)
        assertEquals(CanvasDetailLevel.FULL, result.canvasDetailLevel)
    }

    @Test
    fun reduced_capsEffects_evenWithParallaxToggleOn() {
        // Rule 3: REDUCED quality limits effects even if the parallax preference is true.
        val result = resolveEffectiveVisualSettings(inputs(quality = VisualQuality.REDUCED, parallax = true))
        assertFalse(result.parallaxActive)
        assertEquals(20, result.starDensity)
        assertFalse(result.blurAllowed)
        assertEquals(CanvasDetailLevel.SIMPLE, result.canvasDetailLevel)
    }

    @Test
    fun automatic_onLowRamDevice_resolvesToReducedTier() {
        val result = resolveEffectiveVisualSettings(inputs(quality = VisualQuality.AUTOMATIC, lowRam = true))
        assertEquals(20, result.starDensity)
        assertFalse(result.parallaxActive)
    }

    @Test
    fun automatic_onPowerSaveMode_resolvesToReducedTier() {
        val result = resolveEffectiveVisualSettings(inputs(quality = VisualQuality.AUTOMATIC, powerSave = true))
        assertEquals(20, result.starDensity)
    }

    @Test
    fun automatic_onRegularDevice_resolvesToFullTier() {
        val result = resolveEffectiveVisualSettings(inputs(quality = VisualQuality.AUTOMATIC))
        assertEquals(44, result.starDensity)
    }

    @Test
    fun systemReducedMotion_winsOverFullQualityAndParallaxOn() {
        // Rule 1/4: system-level reduced motion always wins, even under FULL quality.
        val result = resolveEffectiveVisualSettings(
            inputs(quality = VisualQuality.FULL, parallax = true, systemReducedMotion = true)
        )
        assertFalse(result.parallaxActive)
        assertEquals(0f, result.animationDurationScale, 0.0001f)
    }

    @Test
    fun animationsDisabled_disablesParallaxToo() {
        // Rule 2: animationsEnabled = false disables reactive/ambient parallax.
        val result = resolveEffectiveVisualSettings(inputs(animations = false))
        assertFalse(result.parallaxActive)
        assertEquals(0f, result.animationDurationScale, 0.0001f)
    }

    @Test
    fun parallaxPreferenceOff_disablesParallax_evenUnderFullQuality() {
        val result = resolveEffectiveVisualSettings(inputs(quality = VisualQuality.FULL, parallax = false))
        assertFalse(result.parallaxActive)
    }

    @Test
    fun appNotVisible_neverActivatesParallax() {
        // Rule 6: app in background pauses animations and parallax, regardless of every other rule.
        val result = resolveEffectiveVisualSettings(inputs(quality = VisualQuality.FULL, parallax = true, visible = false))
        assertFalse(result.parallaxActive)
        assertFalse(result.ambientMotionActive)
    }

    @Test
    fun explicitReducedOrFull_ignoresDeviceSignals() {
        // Rule 5: AUTOMATIC is the only tier that looks at device/battery - explicit choices win.
        val forcedFull = resolveEffectiveVisualSettings(inputs(quality = VisualQuality.FULL, lowRam = true, powerSave = true))
        assertEquals(44, forcedFull.starDensity)

        val forcedReduced = resolveEffectiveVisualSettings(inputs(quality = VisualQuality.REDUCED, lowRam = false, powerSave = false))
        assertEquals(20, forcedReduced.starDensity)
    }
}
