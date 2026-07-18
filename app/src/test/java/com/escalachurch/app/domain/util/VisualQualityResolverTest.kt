package com.escalachurch.app.domain.util

import com.escalachurch.app.domain.model.VisualQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class VisualQualityResolverTest {

    @Test
    fun full_alwaysFull_regardlessOfDevice() {
        assertEquals(EffectiveVisualQuality.FULL, resolveEffectiveVisualQuality(VisualQuality.FULL, isLowRamDevice = true))
        assertEquals(EffectiveVisualQuality.FULL, resolveEffectiveVisualQuality(VisualQuality.FULL, isLowRamDevice = false))
    }

    @Test
    fun reduced_alwaysReduced_regardlessOfDevice() {
        assertEquals(EffectiveVisualQuality.REDUCED, resolveEffectiveVisualQuality(VisualQuality.REDUCED, isLowRamDevice = true))
        assertEquals(EffectiveVisualQuality.REDUCED, resolveEffectiveVisualQuality(VisualQuality.REDUCED, isLowRamDevice = false))
    }

    @Test
    fun automatic_onLowRamDevice_resolvesToReduced() {
        assertEquals(EffectiveVisualQuality.REDUCED, resolveEffectiveVisualQuality(VisualQuality.AUTOMATIC, isLowRamDevice = true))
    }

    @Test
    fun automatic_onRegularDevice_resolvesToFull() {
        assertEquals(EffectiveVisualQuality.FULL, resolveEffectiveVisualQuality(VisualQuality.AUTOMATIC, isLowRamDevice = false))
    }
}
