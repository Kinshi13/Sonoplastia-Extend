package com.escalachurch.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.escalachurch.app.ui.theme.ConstellationColors
import com.escalachurch.app.ui.theme.ConstellationMotion
import kotlin.random.Random

private data class Star(val xFraction: Float, val yFraction: Float, val radius: Float, val depth: Float)

/**
 * A faint, ambient starfield drawn behind a screen's content - Fase 7's Android parallax, applied
 * to Início. [scrollFraction] is the live carousel offset from [CardCarousel] (already 0-cost
 * while idle, see that file's doc comment); each star drifts horizontally by
 * `scrollFraction * depth`, so farther/dimmer stars (low [Star.depth]) barely move while
 * closer/brighter ones shift more - an actual depth cue, not a screen-wide pan. No timer, no
 * sensor: it only redraws when the user is actively swiping the carousel above it, and this
 * composable never outlives the screen it's declared in (no singleton, nothing to leak).
 *
 * [reducedMotion] freezes the drift entirely (stars render at their rest position) - same
 * convention as everywhere else in Stella Core / Constellation Calm.
 */
@Composable
fun ParallaxStarfield(
    scrollFraction: Float,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    starCount: Int = 36
) {
    val stars = remember(starCount) {
        val random = Random(4242) // fixed seed: same sky every launch, not random noise
        List(starCount) {
            Star(
                xFraction = random.nextFloat(),
                yFraction = random.nextFloat(),
                radius = random.nextFloat() * 1.6f + 0.6f,
                depth = random.nextFloat() * 0.8f + 0.2f
            )
        }
    }

    val animatedFraction = remember { Animatable(0f) }
    LaunchedEffect(scrollFraction, reducedMotion) {
        if (reducedMotion) {
            animatedFraction.snapTo(0f)
        } else {
            animatedFraction.animateTo(scrollFraction, tween(ConstellationMotion.QUICK_MS, easing = ConstellationMotion.stellarEase))
        }
    }

    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
    val starColor = if (isDark) ConstellationColors.Dark.starlight else ConstellationColors.Light.stardust

    Canvas(modifier = modifier.fillMaxSize()) {
        val driftPx = size.width * 0.05f // subtle - a depth cue, not a moving wallpaper
        stars.forEach { star ->
            val x = (star.xFraction * size.width + animatedFraction.value * driftPx * star.depth).mod(size.width)
            val y = star.yFraction * size.height
            drawCircle(
                color = starColor.copy(alpha = 0.10f + star.depth * 0.14f),
                radius = star.radius,
                center = Offset(x, y)
            )
        }
    }
}
