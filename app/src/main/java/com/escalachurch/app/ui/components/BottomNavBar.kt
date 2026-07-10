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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import com.escalachurch.app.ui.stellacore.FourPointStar
import com.escalachurch.app.ui.theme.ConstellationColors
import com.escalachurch.app.ui.theme.ConstellationMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class NavEntry(val destination: AppDestination?, val label: String, val icon: ImageVector?)

// Início's old slot is now Stella Core itself (destination/icon = null - it's not a plain nav
// button anymore, see the STELLA_CORE_INDEX handling below). Same left-to-right order still
// defines swipe-navigation order: swiping into this slot still lands on Início, only a *tap*
// there behaves differently (open the menu / double-tap home) - see the shared gesture handler.
private val navEntries = listOf(
    NavEntry(AppDestination.Program, "Programar", Icons.Filled.EditCalendar),
    NavEntry(AppDestination.Doxology, "Doxologia", Icons.Filled.MusicNote),
    NavEntry(AppDestination.Home, "Início", null),
    NavEntry(AppDestination.Announcements, "Anúncios", Icons.Filled.Campaign),
    NavEntry(AppDestination.Calendar, "Calendário", Icons.Filled.CalendarMonth)
)
private const val STELLA_CORE_INDEX = 2

private const val TAP_SLOP_DP = 12f
private const val SWIPE_THRESHOLD_DP = 56f
private const val DOUBLE_TAP_WINDOW_MS = 300L
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
 * The center slot (previously a plain Início icon) is now Stella Core itself: a single tap there
 * opens its contextual action fan instead of navigating, and a quick double tap goes straight to
 * Início - see the [STELLA_CORE_INDEX] branch inside the shared gesture handler below. Every
 * other slot's tap/swipe behavior is unchanged.
 */
@Composable
fun EscalaBottomNavBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    stellaOpen: Boolean = false,
    onStellaOpenChange: (Boolean) -> Unit = {}
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val tapSlopPx = with(density) { TAP_SLOP_DP.dp.toPx() }
    val swipeThresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }

    val container = rememberAppContainer()
    val settings by container.settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var lastStellaTapAt by remember { mutableLongStateOf(0L) }
    var pendingOpenJob by remember { mutableStateOf<Job?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().height(84.dp)
    ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSizeCompat()) {
                val slotWidth = maxWidth / navEntries.size
                val selectedIndex = navEntries.indexOfFirst { it.destination == currentDestination }.let { if (it == -1) STELLA_CORE_INDEX else it }
                val showIndicator = selectedIndex != STELLA_CORE_INDEX
                val indicatorX by animateDpAsState(
                    targetValue = slotWidth * selectedIndex + slotWidth / 2 - REGULAR_INDICATOR_SIZE / 2,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                    label = "navIndicatorX"
                )

                if (showIndicator) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = indicatorX)
                            .size(REGULAR_INDICATOR_SIZE)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .pointerInput(navEntries, currentDestination) {
                            var startX = 0f
                            var totalDx = 0f
                            detectHorizontalTapOrSwipe(
                                onDown = { offset -> startX = offset.x; totalDx = 0f },
                                onDrag = { dx -> totalDx += dx },
                                onRelease = { widthPx ->
                                    when {
                                        abs(totalDx) > swipeThresholdPx -> {
                                            val currentIndex = navEntries.indexOfFirst { it.destination == currentDestination }
                                                .let { if (it == -1) 0 else it }
                                            val nextIndex = if (totalDx < 0) currentIndex + 1 else currentIndex - 1
                                            navEntries.getOrNull(nextIndex)?.destination?.let { destination ->
                                                onStellaOpenChange(false)
                                                AppSoundPlayer.playSwipeEffect(context, settings.effectsVolume)
                                                onNavigate(destination)
                                            }
                                        }
                                        abs(totalDx) <= tapSlopPx -> {
                                            val tapSlotWidth = widthPx / navEntries.size
                                            val index = (startX / tapSlotWidth).toInt().coerceIn(0, navEntries.size - 1)
                                            if (index == STELLA_CORE_INDEX) {
                                                if (stellaOpen) {
                                                    onStellaOpenChange(false)
                                                } else {
                                                    val now = System.currentTimeMillis()
                                                    if (now - lastStellaTapAt < DOUBLE_TAP_WINDOW_MS) {
                                                        pendingOpenJob?.cancel()
                                                        lastStellaTapAt = 0L
                                                        onNavigate(AppDestination.Home)
                                                    } else {
                                                        lastStellaTapAt = now
                                                        pendingOpenJob = scope.launch {
                                                            delay(DOUBLE_TAP_WINDOW_MS)
                                                            onStellaOpenChange(true)
                                                        }
                                                    }
                                                }
                                            } else {
                                                onStellaOpenChange(false)
                                                navEntries[index].destination?.let(onNavigate)
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
                                StellaCoreBarButton(isOpen = stellaOpen, isOnHome = currentDestination == AppDestination.Home)
                            }
                        } else {
                            val selected = currentDestination == entry.destination
                            val tint by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "navIconTint"
                            )
                            // Every slot gets an equal share of the row's width (not just equal visual
                            // gaps, which is all SpaceEvenly guarantees) - the indicator's x position is
                            // computed as slotWidth * index, so the slots it targets must actually be
                            // uniform width, regardless of each icon's own intrinsic size.
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                entry.icon?.let {
                                    Icon(it, contentDescription = entry.label, tint = tint, modifier = Modifier.size(26.dp))
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
private fun StellaCoreBarButton(isOpen: Boolean, isOnHome: Boolean) {
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
                tokens.polarisSoft.copy(alpha = if (isOpen || isOnHome) 0.7f else 0.35f),
                CircleShape
            )
            .semantics {
                role = androidx.compose.ui.semantics.Role.Button
                contentDescription = if (isOpen) "Fechar menu de ações" else "Stella Core - toque para abrir o menu, toque duas vezes para ir ao Início"
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
