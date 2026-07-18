package com.escalachurch.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastRatioTest {

    @Test
    fun blackOnWhite_isMaximumContrast() {
        assertEquals(21.0, wcagContrastRatio(0x000000, 0xFFFFFF), 0.01)
    }

    @Test
    fun sameColor_isMinimumContrast() {
        assertEquals(1.0, wcagContrastRatio(0x336699, 0x336699), 0.01)
    }

    @Test
    fun orderOfArguments_doesNotMatter() {
        val a = wcagContrastRatio(0x333333, 0xEEEEEE)
        val b = wcagContrastRatio(0xEEEEEE, 0x333333)
        assertEquals(a, b, 0.0001)
    }

    // Fase 11.9B Bloco 17 - regression coverage for the day-constellation contrast fix: every
    // DayConstellationKind tint/accent must clear WCAG's 3:1 minimum for graphical objects against
    // both themes' background. Hex literals mirror DayConstellation.kt's tintColor()/accentColor()
    // and ConstellationColors.Light/Dark.nebula - if either changes, re-check this test.
    private val lightBackground = 0xFFFFFF
    private val darkBackground = 0x12162A // ConstellationColors.Dark.nebula

    @Test
    fun farol_clearsMinimumContrast_inBothThemes() {
        assertTrue(wcagContrastRatio(0x197D8A, lightBackground) >= 3.0)
        assertTrue(wcagContrastRatio(0x5BC8D6, darkBackground) >= 3.0)
    }

    @Test
    fun aurora_clearsMinimumContrast_inBothThemes() {
        assertTrue(wcagContrastRatio(0x1659D4, lightBackground) >= 3.0)
        assertTrue(wcagContrastRatio(0xA9C2F0, darkBackground) >= 3.0)
    }

    @Test
    fun coroaGoldAccent_clearsMinimumContrast_inBothThemes() {
        assertTrue(wcagContrastRatio(0x8B6718, lightBackground) >= 3.0)
        assertTrue(wcagContrastRatio(0xD4A94A, darkBackground) >= 3.0)
    }

    @Test
    fun coroaAndPeregrina_alreadyClearedMinimumContrast_unchanged() {
        assertTrue(wcagContrastRatio(0x3A5AC9, lightBackground) >= 3.0)
        assertTrue(wcagContrastRatio(0x8B6BE0, lightBackground) >= 3.0)
    }

    // Fase 11.9B Bloco 17 - Theme.kt's errorContainer/onErrorContainer (ErrorBanner's text-on-fill
    // pairing) need the stricter 4.5:1 text-contrast minimum, not just the 3:1 graphical one.
    @Test
    fun errorContainerText_clearsTextContrastMinimum_inBothThemes() {
        assertTrue(wcagContrastRatio(0x6C1422, 0xF8E0E4) >= 4.5) // light
        assertTrue(wcagContrastRatio(0xF3AFBA, 0x5C3246) >= 4.5) // dark
    }
}
