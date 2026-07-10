package com.escalachurch.app.ui.stellacore

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.escalachurch.app.ui.theme.ConstellationColors
import com.escalachurch.app.ui.theme.ConstellationMotion
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val NODE_SIZE = 78.dp
private val ARC_RADIUS = 96.dp
private const val STAGGER_STEP_MS = 45

/**
 * The fan of action nodes above Stella Core - an upper arc hugging close to the core (Fase 5
 * Section 2: "arco superior... evitar lista vertical, grid"), connected by thin constellation
 * lines. Cards are large rounded squares (icon + label together, not a tiny circle with a
 * separate floating caption) sitting close enough to slightly overlap, matching the density of a
 * real shortcut fan instead of a thin ring of distant dots. A dimming scrim behind the fan closes
 * the menu on tap-outside, without becoming a generic bottom sheet.
 */
@Composable
fun StellaCoreMenu(
    actions: List<ResolvedStellaCoreAction>,
    reducedMotion: Boolean,
    onActionSelected: (ResolvedStellaCoreAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { ARC_RADIUS.toPx() }
    val nodeCount = actions.size
    // Spread nodes across an upward-opening arc: for 1 node, straight up; for more, fan out
    // symmetrically, capped so it never wraps past horizontal (stays a fan, not a full circle).
    // A tighter spread than a plain semicircle so cards sit close/slightly overlapping, not
    // strung out along a thin distant necklace.
    val spreadDegrees = min(130f, 46f + nodeCount * 16f)
    val startAngle = -90f - spreadDegrees / 2f
    val angleStep = if (nodeCount > 1) spreadDegrees / (nodeCount - 1) else 0f

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        val nodeOffsets = remember(nodeCount) {
            (0 until nodeCount).map { i ->
                val angleRad = Math.toRadians((startAngle + angleStep * i).toDouble())
                Offset(x = (cos(angleRad) * radiusPx).toFloat(), y = (sin(angleRad) * radiusPx).toFloat())
            }
        }

        // Constellation lines from the core to each node - drawn progressively unless reduced
        // motion is on (Section 7: "remover desenho progressivo").
        val lineProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
        LaunchedEffect(nodeCount, reducedMotion) {
            if (!reducedMotion) lineProgress.animateTo(1f, tween(ConstellationMotion.STANDARD_MS, easing = ConstellationMotion.stellarEase))
        }
        val isDark = MaterialTheme.colorScheme.background.let { (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f }
        val lineColor = if (isDark) ConstellationColors.Dark.polaris else ConstellationColors.Light.polaris

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(ARC_RADIUS * 2)
        ) {
            val center = Offset(this.size.width / 2f, this.size.height)
            nodeOffsets.forEach { offset ->
                val target = Offset(center.x + offset.x, center.y + offset.y)
                val drawnTarget = Offset(
                    x = center.x + (target.x - center.x) * lineProgress.value,
                    y = center.y + (target.y - center.y) * lineProgress.value
                )
                drawLine(color = lineColor.copy(alpha = 0.4f), start = center, end = drawnTarget, strokeWidth = 2.5f)
            }
        }

        actions.forEachIndexed { index, resolved ->
            val offset = nodeOffsets[index]
            StellaCoreNode(
                resolved = resolved,
                offsetDp = with(density) { androidx.compose.ui.unit.DpOffset(offset.x.toDp(), offset.y.toDp()) },
                staggerDelayMs = if (reducedMotion) 0 else index * STAGGER_STEP_MS,
                reducedMotion = reducedMotion,
                onClick = { onActionSelected(resolved) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun StellaCoreNode(
    resolved: ResolvedStellaCoreAction,
    offsetDp: androidx.compose.ui.unit.DpOffset,
    staggerDelayMs: Int,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appear = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay(staggerDelayMs.toLong())
            appear.animateTo(1f, tween(ConstellationMotion.STANDARD_MS, easing = ConstellationMotion.stellarEase))
        }
    }
    val isDark = MaterialTheme.colorScheme.background.let { (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f }
    val tokens = if (isDark) ConstellationColors.Dark else ConstellationColors.Light

    Box(
        modifier = modifier.offset(x = offsetDp.x, y = offsetDp.y),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .scale(appear.value)
                .size(NODE_SIZE)
                .clip(RoundedCornerShape(20.dp))
                .background(tokens.nebulaElevated)
                .background((if (resolved.isLocked) tokens.comet else tokens.polaris).copy(alpha = 0.10f))
                .semantics {
                    role = androidx.compose.ui.semantics.Role.Button
                    contentDescription = resolved.action.label + if (resolved.isLocked) " (recurso do plano superior)" else ""
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    resolved.action.icon,
                    contentDescription = null,
                    tint = if (resolved.isLocked) tokens.comet else tokens.polaris,
                    modifier = Modifier.size(26.dp)
                )
                if (resolved.isLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-6).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(tokens.comet),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = tokens.nebula, modifier = Modifier.size(10.dp))
                    }
                }
            }
            Text(
                resolved.action.label,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = tokens.starlight,
                maxLines = 2
            )
        }
    }
}
