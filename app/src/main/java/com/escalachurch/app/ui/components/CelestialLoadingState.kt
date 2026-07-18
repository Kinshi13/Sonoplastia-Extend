package com.escalachurch.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.escalachurch.app.ui.theme.ConstellationColors

/**
 * Fase 11.9B Entrega 3 Bloco 5 - lightweight loading placeholders, replacing a bare
 * CircularProgressIndicator as the only loading response on Home/Escalas/Doxologia/Anúncios.
 * Deliberately static bars (no shimmer/infinite animation per bar) - Bloco 19's "não animar cada
 * card" rule applies directly to a list of skeletons too.
 */
enum class SkeletonVariant { HERO, COMPACT }

@Composable
fun CelestialSkeletonCard(modifier: Modifier = Modifier, variant: SkeletonVariant = SkeletonVariant.COMPACT) {
    val isDark = MaterialTheme.colorScheme.background.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
    val palette = if (isDark) ConstellationColors.Dark else ConstellationColors.Light
    val barColor = palette.horizon

    CelestialFrame(modifier = modifier.fillMaxWidth(), cornerRadius = if (variant == SkeletonVariant.HERO) 28.dp else 18.dp) {
        Column(modifier = Modifier.padding(if (variant == SkeletonVariant.HERO) 24.dp else 16.dp)) {
            SkeletonBar(width = 0.4f, height = 12.dp, color = barColor)
            Spacer(Modifier.height(12.dp))
            SkeletonBar(width = 0.7f, height = if (variant == SkeletonVariant.HERO) 20.dp else 16.dp, color = barColor)
            Spacer(Modifier.height(8.dp))
            SkeletonBar(width = 0.5f, height = 12.dp, color = barColor)
            if (variant == SkeletonVariant.HERO) {
                Spacer(Modifier.height(20.dp))
                repeat(3) {
                    SkeletonBar(width = 0.85f, height = 14.dp, color = barColor)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SkeletonBar(width: Float, height: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth(width)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.4f))
        )
    }
}

/** Full-screen loading response - a Hero skeleton (Home) or a short stack of compact ones
 *  (lists: Escala Geral, Doxologia, Anúncios). Never a bare spinner alone. */
@Composable
fun CelestialLoadingState(
    modifier: Modifier = Modifier,
    variant: SkeletonVariant = SkeletonVariant.COMPACT,
    count: Int = if (variant == SkeletonVariant.HERO) 1 else 3
) {
    Column(modifier = modifier.fillMaxWidth().padding(4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { CelestialSkeletonCard(variant = variant) }
    }
}

/**
 * Compact inline status - "Sincronizando" / "Atualizado" / "Offline · dados salvos" - a dot +
 * label instead of a large dedicated card (Bloco 16: "não ocupar um card grande da Home").
 * Only the dot pulses (a single animated element, not one per list item - safe under Bloco 19).
 */
enum class SyncState { SYNCED, SYNCING, OFFLINE }

@Composable
fun CelestialSyncIndicator(state: SyncState, modifier: Modifier = Modifier, reducedMotion: Boolean = false) {
    val palette = if (MaterialTheme.colorScheme.background.let { (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f })
        ConstellationColors.Dark else ConstellationColors.Light
    val (dotColor, label) = when (state) {
        SyncState.SYNCED -> palette.polaris to "Sincronizado"
        SyncState.SYNCING -> palette.aurora to "Sincronizando…"
        SyncState.OFFLINE -> palette.stardust to "Offline · usando dados salvos"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sync-dot")
    val alpha by if (state == SyncState.SYNCING && !reducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
            label = "sync-dot-alpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = alpha))
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
