package com.escalachurch.app.domain.util

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Fase 11.9B Bloco 17 - WCAG 2.x relative-luminance contrast ratio between two opaque sRGB colors
 * (each packed as 0xRRGGBB, alpha ignored). Pure/no Android dependency on purpose, so it's usable
 * both from Compose code and from plain JVM unit tests (see ContrastRatioTest and
 * DayConstellation.kt's tintColor/accentColor, which this audit found under WCAG's 3:1 minimum for
 * graphical objects against the light theme's background before Bloco 17).
 */
fun wcagContrastRatio(rgb1: Int, rgb2: Int): Double {
    fun relativeLuminance(rgb: Int): Double {
        fun channel(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = channel((rgb shr 16) and 0xFF)
        val g = channel((rgb shr 8) and 0xFF)
        val b = channel(rgb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
    val l1 = relativeLuminance(rgb1)
    val l2 = relativeLuminance(rgb2)
    return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
}
