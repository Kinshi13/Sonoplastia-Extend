package com.escalachurch.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.escalachurch.app.ui.theme.ConstellationColors
import kotlin.random.Random

/** Which accent the halo/constellation layers lean on - PUBLIC uses the calm primary (polaris),
 *  ADMIN leans toward the gold "comet" accent for a more sober, "trusted" read (Bloco 8). Purely
 *  a tint choice, not a different layout. */
enum class CelestialTone { PUBLIC, ADMIN }

private data class BgStarPoint(val xFraction: Float, val yFraction: Float)

/**
 * Fase 11.9B Bloco 4 - a lightweight, reusable cosmic backdrop: deep gradient (layer 0) + faint
 * distant stars (layer 1, reuses [ParallaxStarfield] at rest - no extra star-drawing logic) +
 * a few faint constellation lines (layer 2) + one soft off-center halo (layer 3), with [content]
 * (layer 4) drawn on top. Deliberately static here (no continuous animation, no timer, no sensor) -
 * this is chrome for a handful of low-traffic screens (entry, admin login), not a hot path that
 * needs its own parallax wiring; see ParallaxStarfield for the one place that's justified today
 * (the Home carousel).
 *
 * Everything is vector/Canvas - no raster background images, per the phase's performance rules.
 */
@Composable
fun CelestialBackground(
    modifier: Modifier = Modifier,
    tone: CelestialTone = CelestialTone.PUBLIC,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
    val palette = if (isDark) ConstellationColors.Dark else ConstellationColors.Light
    val haloColor = if (tone == CelestialTone.ADMIN) palette.comet else palette.polaris

    val linePoints = remember {
        val random = Random(2026) // fixed seed - same faint sky every time, not visual noise
        List(5) { BgStarPoint(random.nextFloat(), random.nextFloat() * 0.6f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.void, palette.nebula)))
    ) {
        ParallaxStarfield(scrollFraction = 0f, reducedMotion = true, starCount = 44)

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Layer 2: a few faint connecting lines - decorative chrome, not a specific day's
            // constellation (those stay scoped to schedule cards, see DayConstellation work).
            val points = linePoints.map { Offset(it.xFraction * size.width, it.yFraction * size.height) }
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = palette.horizon.copy(alpha = 0.35f),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 1f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            points.forEach { p -> drawCircle(color = palette.starlight.copy(alpha = 0.4f), radius = 2.2f, center = p) }

            // Layer 3: one soft halo, off-center - a glow, never a hard-edged shape.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(haloColor.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.12f),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.82f, size.height * 0.12f)
            )
        }

        content()
    }
}
