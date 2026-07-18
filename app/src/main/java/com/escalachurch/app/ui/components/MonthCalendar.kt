package com.escalachurch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Self-contained monthly calendar grid. Days carrying any agenda item receive
 * a small marker dot; the currently selected day is highlighted.
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate?,
    markedDates: Set<LocalDate>,
    onMonthChange: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Mês anterior")
            }
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() } + " " + month.year,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo mês")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        val firstDay = month.atDay(1)
        val leadingBlanks = firstDay.dayOfWeek.value % 7 // Sunday = 0
        val totalDays = month.lengthOfMonth()
        val cells = leadingBlanks + totalDays
        val rows = (cells + 6) / 7

        var dayCounter = 1
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    if (cellIndex < leadingBlanks || dayCounter > totalDays) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = month.atDay(dayCounter)
                        DayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == LocalDate.now(),
                            isMarked = markedDates.contains(date),
                            onClick = { onDaySelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isMarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fase 11.9B Bloco 18 - the clickable target used to be the 36dp visual circle itself, below
    // the 48dp minimum touch target (WCAG 2.5.5 / Material). The full cell is at least that big in
    // any real month grid, so the click area moved here - the circle keeps its compact look.
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clickable(
                onClick = onClick,
                onClickLabel = "${date.dayOfMonth}"
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> androidx.compose.ui.graphics.Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${date.dayOfMonth}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isMarked) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    }
}
