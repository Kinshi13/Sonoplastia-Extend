package com.escalachurch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.escalachurch.app.audio.AppSoundPlayer
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.ui.navigation.AppDestination
import kotlin.math.abs

private data class NavEntry(val destination: AppDestination, val label: String, val icon: ImageVector)

// Início sits in the middle of the row (2 items to its left, 3 to its right) instead of first,
// per design guidance to center it. This same order also defines swipe-navigation order below.
private val navEntries = listOf(
    NavEntry(AppDestination.Doxology, "Doxologia", Icons.Filled.MusicNote),
    NavEntry(AppDestination.Program, "Programar", Icons.Filled.EditCalendar),
    NavEntry(AppDestination.Home, "Início", Icons.Filled.Home),
    NavEntry(AppDestination.Calendar, "Calendário", Icons.Filled.CalendarMonth),
    NavEntry(AppDestination.Announcements, "Anúncios", Icons.Filled.Campaign),
    NavEntry(AppDestination.Settings, "Ajustes", Icons.Filled.Settings)
)

private const val TAP_SLOP_DP = 12f
private const val SWIPE_THRESHOLD_DP = 56f

/**
 * Icon-only bottom bar (no labels - at this font size, the six tab names wrapped awkwardly).
 * Bigger touch targets now that there's no label competing for vertical space. A single custom
 * gesture handler covers the whole bar so a horizontal swipe anywhere on it moves to the
 * previous/next tab (in [navEntries] order), while a tap with little movement still selects
 * whichever icon is under the finger - this has to be one unified pointerInput block rather than
 * per-item clickables plus a separate swipe detector, since a child's click-cancel-on-move and a
 * parent's drag detector fight over the same touch stream otherwise.
 */
@Composable
fun EscalaBottomNavBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit
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
        modifier = Modifier.fillMaxWidth().height(92.dp)
    ) {
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
                                    navEntries.getOrNull(nextIndex)?.let {
                                        AppSoundPlayer.playSwipeEffect(context, settings.effectsVolume)
                                        onNavigate(it.destination)
                                    }
                                }
                                abs(totalDx) <= tapSlopPx -> {
                                    val slotWidth = widthPx / navEntries.size
                                    val index = (startX / slotWidth).toInt().coerceIn(0, navEntries.size - 1)
                                    onNavigate(navEntries[index].destination)
                                }
                                else -> Unit // ambiguous drag distance - ignore to avoid accidental navigation
                            }
                        }
                    )
                },
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navEntries.forEach { entry ->
                val selected = currentDestination == entry.destination
                val isHome = entry.destination == AppDestination.Home
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(if (isHome) 52.dp else 40.dp)
                            .let {
                                if (selected) it.background(MaterialTheme.colorScheme.primaryContainer, CircleShape) else it
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            entry.icon,
                            contentDescription = entry.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (isHome) 30.dp else 26.dp)
                        )
                    }
                }
            }
        }
    }
}

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
