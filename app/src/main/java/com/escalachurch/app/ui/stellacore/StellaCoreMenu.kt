package com.escalachurch.app.ui.stellacore

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.escalachurch.app.ui.components.CelestialFrame
import com.escalachurch.app.ui.components.CelestialTone
import com.escalachurch.app.ui.theme.ConstellationColors
import com.escalachurch.app.ui.theme.ConstellationMotion
import kotlinx.coroutines.delay

private val NODE_SIZE = 78.dp
private const val STAGGER_STEP_MS = 45

// Two-branch geometry (Fase 11.9B Bloco 8): each branch climbs up and outward from the core, one
// node per "level". base/step tuned so 3 levels (the max - 6 actions cap, see
// StellaCoreBranchLayout) stay comfortably clear of a 84dp bottom bar on a typical phone.
private val BRANCH_BASE_DX = 34.dp
private val BRANCH_STEP_DX = 26.dp
private val BRANCH_BASE_DY = 78.dp
private val BRANCH_STEP_DY = 80.dp
private val OVERFLOW_DY = 320.dp

/**
 * The two-branch constellation of action nodes above Stella Core (Bloco 8): each branch climbs up
 * and slightly outward from the core, cards connected by a chain of constellation lines rather
 * than a single flat arc - "a composição pode ser levemente assimétrica" when the action count is
 * odd. Cards are large rounded squares (icon + label together), each capped with a small
 * "estrela-nó" where its connector line arrives. A dimming scrim behind the fan closes the menu on
 * tap-outside. At most 6 actions ever reach the branches - beyond that, a single "Mais" node opens
 * the rest as a plain list (Bloco 8: "acima de 6, usar Mais").
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
    val assignment = remember(actions) { assignToBranches(actions) }
    var showOverflow by remember { mutableStateOf(false) }

    // branchSign: -1 for left, +1 for right - dx grows with each level so the branch fans
    // outward as it climbs, never straight vertical (Bloco 8's reference diagram).
    fun offsetFor(branchSign: Int, levelInBranch: Int): Offset {
        val dxDp = BRANCH_BASE_DX.value + BRANCH_STEP_DX.value * levelInBranch
        val dyDp = -(BRANCH_BASE_DY.value + BRANCH_STEP_DY.value * levelInBranch)
        return Offset(branchSign * dxDp, dyDp)
    }

    val leftOffsets = assignment.left.indices.map { offsetFor(-1, it) }
    val rightOffsets = assignment.right.indices.map { offsetFor(1, it) }
    val overflowOffset = Offset(0f, -OVERFLOW_DY.value)

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

        // Constellation lines: core -> first node of each branch, then node -> next node along
        // the same branch (a chain, not independent spokes) - drawn progressively unless reduced
        // motion is on.
        val lineProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
        LaunchedEffect(actions.size, reducedMotion) {
            if (!reducedMotion) lineProgress.animateTo(1f, tween(ConstellationMotion.STANDARD_MS, easing = ConstellationMotion.stellarEase))
        }
        val isDark = MaterialTheme.colorScheme.background.let { (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f }
        val lineColor = if (isDark) ConstellationColors.Dark.polaris else ConstellationColors.Light.polaris

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
        ) {
            val center = Offset(this.size.width / 2f, this.size.height)

            fun drawProgressiveLine(from: Offset, to: Offset) {
                val drawnTo = Offset(
                    x = from.x + (to.x - from.x) * lineProgress.value,
                    y = from.y + (to.y - from.y) * lineProgress.value
                )
                drawLine(color = lineColor.copy(alpha = 0.4f), start = from, end = drawnTo, strokeWidth = 2.5f)
            }

            fun chain(offsets: List<Offset>) {
                var previous = center
                offsets.forEach { offset ->
                    val target = Offset(center.x + offset.x, center.y + offset.y)
                    drawProgressiveLine(previous, target)
                    previous = target
                }
            }
            chain(leftOffsets)
            chain(rightOffsets)
            if (assignment.overflow.isNotEmpty()) {
                drawProgressiveLine(center, Offset(center.x + overflowOffset.x, center.y + overflowOffset.y))
            }
        }

        var globalIndex = 0
        assignment.left.forEachIndexed { i, resolved ->
            StellaCoreNode(
                resolved = resolved,
                offsetDp = leftOffsets[i].toDpOffset(density),
                staggerDelayMs = if (reducedMotion) 0 else globalIndex++ * STAGGER_STEP_MS,
                reducedMotion = reducedMotion,
                onClick = { onActionSelected(resolved) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        assignment.right.forEachIndexed { i, resolved ->
            StellaCoreNode(
                resolved = resolved,
                offsetDp = rightOffsets[i].toDpOffset(density),
                staggerDelayMs = if (reducedMotion) 0 else globalIndex++ * STAGGER_STEP_MS,
                reducedMotion = reducedMotion,
                onClick = { onActionSelected(resolved) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        if (assignment.overflow.isNotEmpty()) {
            StellaCoreOverflowNode(
                offsetDp = overflowOffset.toDpOffset(density),
                staggerDelayMs = if (reducedMotion) 0 else globalIndex * STAGGER_STEP_MS,
                reducedMotion = reducedMotion,
                onClick = { showOverflow = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showOverflow) {
        AlertDialog(
            onDismissRequest = { showOverflow = false },
            title = { Text("Mais ações") },
            text = {
                Column {
                    assignment.overflow.forEach { resolved ->
                        TextButton(onClick = { showOverflow = false; onActionSelected(resolved) }) {
                            Icon(resolved.action.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(resolved.action.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOverflow = false }) { Text("Fechar") }
            }
        )
    }
}

private fun Offset.toDpOffset(density: androidx.compose.ui.unit.Density): androidx.compose.ui.unit.DpOffset =
    with(density) { androidx.compose.ui.unit.DpOffset(x.dp, y.dp) }

@Composable
private fun StellaCoreOverflowNode(
    offsetDp: androidx.compose.ui.unit.DpOffset,
    staggerDelayMs: Int,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    StellaCoreNodeShell(offsetDp = offsetDp, staggerDelayMs = staggerDelayMs, reducedMotion = reducedMotion, modifier = modifier) { appear, tokens ->
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(100), label = "stellaCoreNodePress")
        CelestialFrame(
            modifier = Modifier
                .scale(appear * pressScale)
                .size(NODE_SIZE)
                .semantics {
                    role = androidx.compose.ui.semantics.Role.Button
                    contentDescription = "Mais ações"
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            tone = CelestialTone.PUBLIC,
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = tokens.polaris, modifier = Modifier.size(26.dp))
                Text("Mais", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = tokens.starlight)
            }
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
    StellaCoreNodeShell(offsetDp = offsetDp, staggerDelayMs = staggerDelayMs, reducedMotion = reducedMotion, modifier = modifier) { appear, tokens ->
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(100), label = "stellaCoreNodePress")
        // Fase 11.9B Bloco 9 - subtitle is hidden below this width rather than shrinking either
        // line's font ("no mobile pequeno, ocultar subtítulo antes de reduzir a fonte").
        val showSubtitle = resolved.action.subtitle != null && LocalConfiguration.current.screenWidthDp >= 360
        CelestialFrame(
            modifier = Modifier
                .scale(appear * pressScale)
                .size(NODE_SIZE)
                .semantics {
                    role = androidx.compose.ui.semantics.Role.Button
                    contentDescription = resolved.action.label + if (resolved.isLocked) " (recurso do plano superior)" else ""
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            tone = if (resolved.isLocked) CelestialTone.ADMIN else CelestialTone.PUBLIC,
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
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
                if (showSubtitle) {
                    Text(
                        resolved.action.subtitle.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = tokens.stardust,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Shared shell: positions the node, runs the staggered appear animation, resolves the palette,
 *  and draws the "estrela-nó" - both [StellaCoreNode] and [StellaCoreOverflowNode] just supply
 *  their own card content. */
@Composable
private fun StellaCoreNodeShell(
    offsetDp: androidx.compose.ui.unit.DpOffset,
    staggerDelayMs: Int,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (appear: Float, tokens: com.escalachurch.app.ui.theme.ConstellationPalette) -> Unit
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
        FourPointStar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-7).dp)
                .size(11.dp)
                .scale(appear.value),
            color = tokens.polaris,
            glowColor = tokens.comet
        )
        content(appear.value, tokens)
    }
}
