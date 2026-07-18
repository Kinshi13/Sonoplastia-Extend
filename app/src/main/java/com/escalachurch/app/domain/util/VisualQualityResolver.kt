package com.escalachurch.app.domain.util

import com.escalachurch.app.domain.model.VisualQuality

/** What decorative code actually renders with, after AUTOMATIC has been resolved against the
 *  device - screens should never branch on [VisualQuality] directly. */
enum class EffectiveVisualQuality { REDUCED, FULL }

/**
 * Fase 11.9B Bloco 12 - AUTOMATIC picks REDUCED on a device Android itself already flags as low-RAM
 * (ActivityManager.isLowRamDevice - no benchmarking, no new permission, matches what the OS uses
 * to decide whether to enable its own background/animation restrictions). REDUCED/FULL are
 * explicit user choices and always win regardless of the device.
 *
 * Pure - see VisualQualityResolverTest.
 */
fun resolveEffectiveVisualQuality(preference: VisualQuality, isLowRamDevice: Boolean): EffectiveVisualQuality =
    when (preference) {
        VisualQuality.FULL -> EffectiveVisualQuality.FULL
        VisualQuality.REDUCED -> EffectiveVisualQuality.REDUCED
        VisualQuality.AUTOMATIC -> if (isLowRamDevice) EffectiveVisualQuality.REDUCED else EffectiveVisualQuality.FULL
    }
