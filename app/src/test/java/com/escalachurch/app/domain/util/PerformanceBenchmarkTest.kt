package com.escalachurch.app.domain.util

import com.escalachurch.app.domain.model.VisualQuality
import com.escalachurch.app.ui.stellacore.assignToBranches
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 11.9B Bloco 19 - "benchmarks simples" for the pure functions on the hottest paths
 * (recomposition-adjacent: called from Composable bodies on every relevant recomposition, not
 * behind a button press). This sandbox has no emulator/adb, so there's no real on-device frame-
 * timing benchmark (Macrobenchmark) here - this only proves the *algorithmic* cost of the pure
 * logic itself is negligible, which is what's actually testable without a device. Thresholds are
 * generous on purpose (sanity checks against a regression, e.g. an accidental O(n^2)/allocation-
 * per-call bug - not tight perf assertions that would flake on a loaded CI box).
 */
class PerformanceBenchmarkTest {

    private fun benchmark(label: String, iterations: Int, block: () -> Unit): Double {
        repeat(1000) { block() } // warm up JIT before timing
        val start = System.nanoTime()
        repeat(iterations) { block() }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        val perCallUs = (elapsedMs * 1000) / iterations
        println("[Bloco 19 benchmark] $label: $iterations calls in ${"%.2f".format(elapsedMs)}ms (${"%.3f".format(perCallUs)}us/call)")
        return perCallUs
    }

    @Test
    fun resolveEffectiveVisualSettings_isNegligibleCost() {
        val inputs = VisualQualityInputs(
            qualityPreference = VisualQuality.AUTOMATIC,
            parallaxPreference = true,
            animationsEnabled = true,
            systemReducedMotion = false,
            isLowRamDevice = false,
            isPowerSaveMode = false,
            isAppVisible = true
        )
        val perCallUs = benchmark("resolveEffectiveVisualSettings", 100_000) {
            resolveEffectiveVisualSettings(inputs)
        }
        assertTrue("resolveEffectiveVisualSettings should stay well under 5us/call, was ${perCallUs}us", perCallUs < 5.0)
    }

    @Test
    fun assignToBranches_isNegligibleCost_evenAtMaxActionCount() {
        val actions = (1..6).map { "action-$it" }
        val perCallUs = benchmark("assignToBranches(6 actions)", 100_000) {
            assignToBranches(actions)
        }
        assertTrue("assignToBranches should stay well under 5us/call, was ${perCallUs}us", perCallUs < 5.0)
    }

    @Test
    fun wcagContrastRatio_isNegligibleCost() {
        val perCallUs = benchmark("wcagContrastRatio", 100_000) {
            wcagContrastRatio(0x1659D4, 0xFFFFFF)
        }
        assertTrue("wcagContrastRatio should stay well under 5us/call, was ${perCallUs}us", perCallUs < 5.0)
    }

    @Test
    fun friendlyErrorMessage_isNegligibleCost() {
        val error = RuntimeException("PGRST301: connection refused at 10.0.0.5:5432")
        val perCallUs = benchmark("friendlyErrorMessage", 100_000) {
            friendlyErrorMessage(error, "fallback")
        }
        assertTrue("friendlyErrorMessage should stay well under 5us/call, was ${perCallUs}us", perCallUs < 5.0)
    }
}
