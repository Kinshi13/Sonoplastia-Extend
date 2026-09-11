package com.escalachurch.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.escalachurch.app.audio.AppSoundPlayer
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.ui.navigation.AppDestination
import com.escalachurch.app.ui.navigation.SecondaryDestination
import com.escalachurch.app.ui.stellacore.FourPointStar
import com.escalachurch.app.ui.theme.ConstellationColors
import com.escalachurch.app.ui.theme.ConstellationMotion
import kotlin.math.abs

/**
 * [navigateRoute] is the concrete route passed to [onNavigate] when this slot is tapped/swiped
 * into; [matchRoute] is what a real current route is compared against to decide if this slot is
 * selected. They differ only for Escalas: Navigation Compose reports a parameterized
 * destination's *template* as the current route (e.g. "general_scale?date={date}"), never the
 * filled-in value actually navigated to - see [SecondaryDestination.GENERAL_SCALE_ROUTE].
 */
private data class NavEntry(val navigateRoute: String?, val matchRoute: String?, val label: String, val icon: ImageVector?)

// Fase 11.11 - HOME | ESCALAS | (Stella Core) | ANÚNCIOS | CALENDÁRIO. The star is no longer a
// stand-in for Início: it has no route of its own (navigateRoute/matchRoute = null) and never
// navigates - see STELLA_CORE_INDEX handling below. Início now owns its own slot, reachable with
// a single, immediate tap like every other slot.
private val navEntries = listOf(
    NavEntry(AppDestination.Home.route, AppDestination.Home.route, "Início", Icons.Filled.Home),
    NavEntry(SecondaryDestination.generalScaleRoute(null), SecondaryDestination.GENERAL_SCALE_ROUTE, "Escalas", Icons.Filled.EditCalendar),
    NavEntry(null, null, "Stella Core", null),
    NavEntry(AppDestination.Announcements.route, AppDestination.Announcements.route, "Anúncios", Icons.Filled.Campaign),
    NavEntry(AppDestination.Calendar.route, AppDestination.Calendar.route, "Calendário", Icons.Filled.CalendarMonth)
)
private const val STELLA_CORE_INDEX = 2

private const val TAP_SLOP_DP = 12f
private const val SWIPE_THRESHOLD_DP = 56f
private val REGULAR_INDICATOR_SIZE = 40.dp
private val STELLA_CORE_SIZE = 40.dp

/**
 * Icon-only bottom bar (no labels - at this font size, the six tab names wrapped awkwardly).
 * Bigger touch targets now that there's no label competing for vertical space. A single custom
 * gesture handler covers the whole bar so a horizontal swipe anywhere on it moves to the
 * previous/next tab (in [navEntries] order), while a tap with little movement still selects
 * whichever icon is under the finger - this has to be one unified pointerInput block rather than
 * per-item clickables plus a separate swipe detector, since a child's click-cancel-on-move and a
 * parent's drag detector fight over the same touch stream otherwise.
 *
 * The center slot is Stella Core itself, not a destination: a single, immediate tap there opens
 * its contextual action fan, tapping again closes it - it never navigates anywhere, Início included
 * (see the [STELLA_CORE_INDEX] branch inside the shared gesture handler below). Every other slot
 * (Início among them) is a normal single-tap destination.
 */
@Composable
fun EscalaBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    stellaOpen: Boolean = false,
    onStellaOpenChange: (Boolean) -> Unit = {}
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val tapSlopPx = with(density) { TAP_SLOP_DP.dp.toPx() }
    val swipeThresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }

    val container = rememberAppContainer()
    val settings by container.settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
    val context = androidx.compose.ui.platform.LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().height(84.dp)
    ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSizeCompat()) {
                val slotWidth = maxWidth / navEntries.size
                val selectedIndex = navEntries.indexOfFirst { it.matchRoute != null && it.matchRoute == currentRoute }
                    .let { if (it == -1) STELLA_CORE_INDEX else it }
                val showIndicator = selectedIndex != STELLA_CORE_INDEX
                val indicatorX by animateDpAsState(
                    targetValue = slotWidth * selectedIndex + slotWidth / 2 - REGULAR_INDICATOR_SIZE / 2,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                    label = "navIndicatorX"
                )

                if (showIndicator) {
                    val indicatorAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (stellaOpen) 0.35f else 1f,
                        animationSpec = androidx.compose.animation.core.tween(ConstellationMotion.STANDARD_MS),
                        label = "navIndicatorDim"
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = indicatorX)
                            .size(REGULAR_INDICATOR_SIZE)
                            .graphicsLayer { alpha = indicatorAlpha }
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .pointerInput(navEntries, currentRoute) {
                            var startX = 0f
                            var totalDx = 0f
                            detectHorizontalTapOrSwipe(
                                onDown = { offset -> startX = offset.x; totalDx = 0f },
                                onDrag = { dx -> totalDx += dx },
                                onRelease = { widthPx ->
                                    when {
                                        abs(totalDx) > swipeThresholdPx -> {
                                            val currentIndex = navEntries.indexOfFirst { it.matchRoute != null && it.matchRoute == currentRoute }
                                                .let { if (it == -1) 0 else it }
                                            val nextIndex = if (totalDx < 0) currentIndex + 1 else currentIndex - 1
                                            navEntries.getOrNull(nextIndex)?.navigateRoute?.let { route ->
                                                onStellaOpenChange(false)
                                                AppSoundPlayer.playSwipeEffect(context, settings.effectsVolume)
                                                onNavigate(route)
                                            }
                                        }
                                        abs(totalDx) <= tapSlopPx -> {
                                            val tapSlotWidth = widthPx / navEntries.size
                                            val index = (startX / tapSlotWidth).toInt().coerceIn(0, navEntries.size - 1)
                                            if (index == STELLA_CORE_INDEX) {
                                                // Fase 11.11 - "A ESTRELA NÃO É HOME": a pure, immediate
                                                // toggle. No timing window, no double-tap-to-Início - that
                                                // delay used to be the whole reason a single tap here felt
                                                // slow. Início now has its own slot (index 0).
                                                onStellaOpenChange(!stellaOpen)
                                            } else {
                                                onStellaOpenChange(false)
                                                navEntries[index].navigateRoute?.let(onNavigate)
                                            }
                                        }
                                        else -> Unit // ambiguous drag distance - ignore to avoid accidental navigation
                                    }
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navEntries.forEachIndexed { index, entry ->
                        if (index == STELLA_CORE_INDEX) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                StellaCoreBarButton(isOpen = stellaOpen)
                            }
                        } else {
                            val selected = entry.matchRoute != null && entry.matchRoute == currentRoute
                            val tint by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "navIconTint"
                            )
                            // Fase 11.9B Bloco 11 - the other tabs stay tappable while Stella Core is
                            // open (tapping one still cancels the menu and switches tab, same gesture
                            // handler as always), but they dim to echo the backdrop's "everything but
                            // the core dims" rule and keep visual focus on the open fan.
                            val iconAlpha by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (stellaOpen) 0.35f else 1f,
                                animationSpec = androidx.compose.animation.core.tween(ConstellationMotion.STANDARD_MS),
                                label = "navIconDim"
                            )
                            // Every slot gets an equal share of the row's width (not just equal visual
                            // gaps, which is all SpaceEvenly guarantees) - the indicator's x position is
                            // computed as slotWidth * index, so the slots it targets must actually be
                            // uniform width, regardless of each icon's own intrinsic size.
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                entry.icon?.let {
                                    Icon(
                                        it,
                                        contentDescription = entry.label,
                                        tint = tint,
                                        modifier = Modifier.size(26.dp).graphicsLayer { alpha = iconAlpha }
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}

/** The closed-state star drawn directly in the nav bar's Início slot - same size class as the
 *  other icons (not the oversized floating orb from before), so it visually replaces rather than
 *  hovers above the button it took over. */
@Composable
private fun StellaCoreBarButton(isOpen: Boolean) {
    val isDark = MaterialTheme.colorScheme.background.let { (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f }
    val tokens = if (isDark) ConstellationColors.Dark else ConstellationColors.Light
    val starRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isOpen) 45f else 0f,
        animationSpec = androidx.compose.animation.core.tween(ConstellationMotion.STANDARD_MS, easing = ConstellationMotion.stellarEase),
        label = "stellaBarRotation"
    )
    Box(
        modifier = Modifier
            .size(STELLA_CORE_SIZE)
            .background(
                tokens.polarisSoft.copy(alpha = if (isOpen) 0.7f else 0.35f),
                CircleShape
            )
            .semantics {
                role = androidx.compose.ui.semantics.Role.Button
                // Fase 11.11 - "★ NÃO É HOME": the star only ever opens/closes Stella Core.
                contentDescription = if (isOpen) "Fechar Stella Core" else "Stella Core - toque para abrir o menu de ações"
            },
        contentAlignment = Alignment.Center
    ) {
        FourPointStar(
            modifier = Modifier.size(22.dp).rotate(starRotation),
            color = tokens.polaris,
            glowColor = tokens.comet
        )
    }
}

private fun Modifier.fillMaxSizeCompat(): Modifier = this.fillMaxWidth().fillMaxHeight()

/**
 * Single gesture recognizer for the whole bar: tracks one pointer from down to up, reporting the
 * down position, cumulative horizontal drag, and the container width at release - callers decide
 * whether that adds up to a tap (for that position) or a swipe (left/right).
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectHorizontalTapOrSwipe(
    onDown: (Offset) -> Unit,
    onDrag: (Float) -> Unit,
    onRelease: (containerWidthPx: Float) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onDown(down.position)
        val pointerId = down.id
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) {
                onRelease(size.width.toFloat())
                break
            }
            val dx = change.position.x - change.previousPosition.x
            if (dx != 0f) onDrag(dx)
            change.consume()
        }
    }
}
