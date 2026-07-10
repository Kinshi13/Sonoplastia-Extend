package com.escalachurch.app.ui.stellacore

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.escalachurch.app.ui.theme.ConstellationColors
import com.escalachurch.app.ui.theme.ConstellationMotion
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val CORE_SIZE = 60.dp
private val NODE_SIZE = 52.dp
private val ARC_RADIUS = 118.dp

/**
 * The Stella Core button: a four-point star, centered and slightly raised above the bottom nav,
 * that opens into a fan of contextual actions (see [StellaCoreMenu]) instead of navigating
 * anywhere itself. This composable owns only open/closed state and the star's own reaction to a
 * tap - screen-specific behavior is entirely supplied by [actions] via StellaCoreConnector.
 *
 * [reducedMotion] mirrors AppSettings.animationsEnabled: when true, the menu appears/disappears
 * immediately with no progressive line-draw or stagger (Fase 5 Section 7).
 */
@Composable
fun StellaCore(
    actions: List<ResolvedStellaCoreAction>,
    reducedMotion: Boolean,
    onLockedActionClick: (StellaCoreAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tokens = if (isDark) ConstellationColors.Dark else ConstellationColors.Light

    val tapScale = remember { Animatable(1f) }
    LaunchedEffect(isOpen) {
        // A single quick pulse on open/close - "core reaction" (Section 5), not a continuous
        // idle animation (Section 1: "sem animação excessiva" while closed).
        tapScale.animateTo(0.88f, tween(90, easing = LinearOutSlowInEasing))
        tapScale.animateTo(1f, tween(160))
    }
    val starRotation by animateFloatAsState(
        targetValue = if (isOpen) 45f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else ConstellationMotion.STANDARD_MS, easing = ConstellationMotion.stellarEase),
        label = "stellaRotation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        if (isOpen && actions.isNotEmpty()) {
            StellaCoreMenu(
                actions = actions,
                reducedMotion = reducedMotion,
                onActionSelected = { resolved ->
                    isOpen = false
                    if (resolved.isLocked) onLockedActionClick(resolved.action) else resolved.action.onClick()
                },
                onDismiss = { isOpen = false }
            )
        }

        Box(
            modifier = Modifier
                .size(CORE_SIZE)
                .offset(y = (-16).dp)
                .scale(tapScale.value)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .background(tokens.polarisSoft.copy(alpha = if (isOpen) 0.9f else 0.55f), CircleShape)
                .semantics {
                    role = androidx.compose.ui.semantics.Role.Button
                    contentDescription = if (isOpen) "Fechar menu de ações" else "Abrir menu de ações (Stella Core)"
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isOpen = !isOpen },
            contentAlignment = Alignment.Center
        ) {
            FourPointStar(
                modifier = Modifier.size(28.dp).rotate(starRotation),
                color = tokens.polaris,
                glowColor = tokens.comet
            )
        }
    }
}

/** A four-point star (not a five-point "sparkle") - Fase 5's explicit geometry, drawn once as a
 *  path rather than pulled in as a generic icon so its silhouette stays exactly this shape. */
@Composable
private fun FourPointStar(modifier: Modifier = Modifier, color: Color, glowColor: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerX = w / 2f
        val outerY = h / 2f
        val innerRatio = 0.32f

        val path = Path().apply {
            moveTo(cx, cy - outerY)
            lineTo(cx + outerX * innerRatio, cy - outerY * innerRatio)
            lineTo(cx + outerX, cy)
            lineTo(cx + outerX * innerRatio, cy + outerY * innerRatio)
            lineTo(cx, cy + outerY)
            lineTo(cx - outerX * innerRatio, cy + outerY * innerRatio)
            lineTo(cx - outerX, cy)
            lineTo(cx - outerX * innerRatio, cy - outerY * innerRatio)
            close()
        }
        drawPath(path, color = glowColor.copy(alpha = 0.25f), style = Stroke(width = 6f))
        drawPath(path, color = color)
    }
}

private fun Color.luminance(): Float = (0.299f * red + 0.587f * green + 0.114f * blue)
