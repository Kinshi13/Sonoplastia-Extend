package com.escalachurch.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.escalachurch.app.domain.model.AppFont

/**
 * Maps an [AppFont] choice to a concrete [FontFamily].
 *
 * The MVP ships with system-provided families only (no bundled font files, so
 * the app has zero extra asset weight). The structure already distinguishes
 * free vs. premium fonts so a future release can drop real font files into
 * res/font and wire them in here without touching call sites.
 */
fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.SYSTEM_DEFAULT -> FontFamily.Default
    AppFont.SERIF_CLASSIC -> FontFamily.Serif
    AppFont.ROUNDED -> FontFamily.SansSerif
    // Premium fonts are not available yet; fall back gracefully to the default.
    AppFont.ELEGANT_SCRIPT -> FontFamily.Default
    AppFont.MODERN_PREMIUM -> FontFamily.Default
}

fun buildTypography(fontFamily: FontFamily, scale: Float): Typography {
    fun style(size: Int, weight: FontWeight, lineHeight: Int, letterSpacing: Float = 0f) = TextStyle(
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = (size * scale).sp,
        lineHeight = (lineHeight * scale).sp,
        letterSpacing = letterSpacing.sp
    )

    return Typography(
        displaySmall = style(34, FontWeight.Bold, 40),
        headlineMedium = style(26, FontWeight.Bold, 32),
        headlineSmall = style(22, FontWeight.SemiBold, 28),
        titleLarge = style(20, FontWeight.SemiBold, 26),
        titleMedium = style(17, FontWeight.SemiBold, 24),
        titleSmall = style(15, FontWeight.SemiBold, 20),
        bodyLarge = style(16, FontWeight.Normal, 24),
        bodyMedium = style(14, FontWeight.Normal, 20),
        bodySmall = style(12, FontWeight.Normal, 16),
        labelLarge = style(14, FontWeight.Medium, 20, 0.2f),
        labelMedium = style(12, FontWeight.Medium, 16, 0.2f),
        labelSmall = style(11, FontWeight.Medium, 14, 0.2f)
    )
}
