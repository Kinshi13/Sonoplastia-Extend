package com.escalachurch.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.domain.model.ProgramStep
import com.escalachurch.app.ui.theme.ConstellationColors
import java.time.LocalDate
import java.time.LocalTime

private enum class StepState { DONE, CURRENT, FUTURE }

/** Which step is "now", based on cumulative estimated duration from [DoxologyItem.startTime] -
 *  only meaningful when the doxology is today; a past date is all DONE, a future date all FUTURE. */
private fun DoxologyItem.stepStates(now: LocalDate = LocalDate.now(), nowTime: LocalTime = LocalTime.now()): Map<Int, StepState> {
    val steps = programOrder.sortedBy { it.order }
    return when {
        date.isBefore(now) -> steps.associate { it.order to StepState.DONE }
        date.isAfter(now) -> steps.associate { it.order to StepState.FUTURE }
        else -> {
            var cursor = startTime
            val result = mutableMapOf<Int, StepState>()
            for (step in steps) {
                val stepEnd = cursor.plusMinutes((step.estimatedDurationMinutes ?: 0).toLong())
                result[step.order] = when {
                    nowTime.isBefore(cursor) -> StepState.FUTURE
                    nowTime.isBefore(stepEnd) || step.estimatedDurationMinutes == null -> StepState.CURRENT
                    else -> StepState.DONE
                }
                cursor = stepEnd
            }
            result
        }
    }
}

/** Fase 11.9B Entrega 3 - CelestialDoxologyCard/Timeline. Flash-card representation of a
 *  [DoxologyItem] (order of service) for the Doxologia screen - now a CelestialFrame with a real
 *  connector+node timeline instead of a plain numbered list, but the same data/reuse logic as
 *  before (see DoxologyViewModel/DoxologyEditScreen, untouched). */
@Composable
fun DoxologyCard(
    doxology: DoxologyItem,
    modifier: Modifier = Modifier,
    isLive: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
    val palette = if (isDark) ConstellationColors.Dark else ConstellationColors.Light
    val steps = doxology.programOrder.sortedBy { it.order }
    val states = doxology.stepStates()

    CelestialFrame(modifier = modifier.fillMaxWidth(), cornerRadius = 28.dp) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Doxologia", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isLive) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Text(
                                "Agora",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    // Doxologia has no isSpecialEvent flag - reuse the same weekday mapping used
                    // for scales so the same day always shows the same constellation.
                    val kind = when (doxology.date.dayOfWeek) {
                        java.time.DayOfWeek.WEDNESDAY -> DayConstellationKind.FAROL
                        java.time.DayOfWeek.SATURDAY -> DayConstellationKind.COROA
                        java.time.DayOfWeek.SUNDAY -> DayConstellationKind.AURORA
                        else -> DayConstellationKind.FAROL
                    }
                    DayConstellationGlyph(
                        kind = kind,
                        modifier = Modifier.size(22.dp),
                        intensity = ConstellationIntensity.COMPACT,
                        tint = kind.tintColor(),
                        accentColor = kind.accentColor()
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(doxology.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "${doxology.date.dayOfWeekLabel()} · ${doxology.date.toDisplayString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                doxology.startTime.toDisplayString() + (doxology.endTime?.let { " - ${it.toDisplayString()}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            steps.forEachIndexed { index, step ->
                DoxologyTimelineStep(
                    step = step,
                    state = states[step.order] ?: StepState.FUTURE,
                    isLast = index == steps.lastIndex,
                    palette = palette
                )
            }

            if (doxology.notes.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(doxology.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DoxologyTimelineStep(
    step: ProgramStep,
    state: StepState,
    isLast: Boolean,
    palette: com.escalachurch.app.ui.theme.ConstellationPalette
) {
    val nodeColor = when (state) {
        StepState.DONE -> palette.stardust
        StepState.CURRENT -> palette.polaris
        StepState.FUTURE -> palette.horizon
    }
    val titleColor = if (state == StepState.FUTURE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Row(modifier = Modifier.fillMaxWidth()) {
        // Node + connector column - a real timeline instead of a numbered list.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(modifier = Modifier.size(if (state == StepState.CURRENT) 14.dp else 10.dp), contentAlignment = Alignment.Center) {
                if (state == StepState.CURRENT) {
                    // Localized glow only on the current step - never on every node (Bloco 2 rule).
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawCircle(color = nodeColor.copy(alpha = 0.28f), radius = size.minDimension / 2f)
                    }
                }
                Canvas(modifier = Modifier.size(if (state == StepState.CURRENT) 8.dp else 8.dp)) {
                    drawCircle(color = nodeColor, radius = size.minDimension / 2f)
                }
            }
            if (!isLast) {
                Canvas(modifier = Modifier.width(2.dp).padding(top = 2.dp).height(48.dp)) {
                    drawLine(
                        color = if (state == StepState.DONE) palette.polaris.copy(alpha = 0.5f) else palette.horizon.copy(alpha = 0.6f),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = size.width,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 14.dp)) {
            Text(step.title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            val meta = buildString {
                if (step.responsiblePerson.isNotBlank()) append(step.responsiblePerson)
                step.estimatedDurationMinutes?.let {
                    if (isNotEmpty()) append(" · ")
                    append("${it} min")
                }
            }
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (step.description.isNotBlank()) {
                Text(step.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
