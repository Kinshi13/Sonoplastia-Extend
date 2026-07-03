package com.escalachurch.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.AgendaEntry
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.ui.components.MonthCalendar
import com.escalachurch.app.ui.components.PulledUpEntrance
import com.escalachurch.app.ui.components.SourceBadge
import com.escalachurch.app.ui.components.dayOfWeekLabel
import com.escalachurch.app.ui.components.rememberEntranceVisible
import com.escalachurch.app.ui.components.toDisplayString
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen() {
    val viewModel = appViewModel { container ->
        CalendarViewModel(container.scaleRepository, container.doxologyRepository, container.customEventRepository, container.announcementRepository)
    }
    val entries by viewModel.entries.collectAsState()
    val appSettings by rememberAppContainer().settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
    val entriesVisible = rememberEntranceVisible(appSettings.animationsEnabled)

    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val markedDates = remember(entries) { entries.map { it.date }.toSet() }
    val dayEntries = remember(entries, selectedDate) {
        // Ordem pedida: Escalas, Doxologia, Eventos, Anúncios (feriados por último).
        entries.filter { it.date == selectedDate }.sortedBy { entryCategoryRank(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Calendário", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))

        Card(shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            MonthCalendar(
                month = month,
                selectedDate = selectedDate,
                markedDates = markedDates,
                onMonthChange = { month = it },
                onDaySelected = { selectedDate = it },
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "${selectedDate.dayOfWeekLabel()}, ${selectedDate.toDisplayString()}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        if (dayEntries.isEmpty()) {
            Text(
                "Nenhuma programação neste dia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            PulledUpEntrance(visible = entriesVisible) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(dayEntries, key = { entryKey(it) }) { entry ->
                        AgendaEntryRow(entry)
                    }
                }
            }
        }
    }
}

private fun entryKey(entry: AgendaEntry): String = when (entry) {
    is AgendaEntry.Scale -> "scale-${entry.scale.id}"
    is AgendaEntry.Doxology -> "doxology-${entry.doxology.id}"
    is AgendaEntry.Event -> "event-${entry.event.id}"
    is AgendaEntry.AnnouncementEntry -> "announcement-${entry.announcement.id}"
    is AgendaEntry.Holiday -> "holiday-${entry.holiday.name}-${entry.holiday.date}"
}

private fun entryCategoryRank(entry: AgendaEntry): Int = when (entry) {
    is AgendaEntry.Scale -> 0
    is AgendaEntry.Doxology -> 1
    is AgendaEntry.Event -> 2
    is AgendaEntry.AnnouncementEntry -> 3
    is AgendaEntry.Holiday -> 4
}

@Composable
private fun AgendaEntryRow(entry: AgendaEntry) {
    val (label, dotColor) = when (entry) {
        is AgendaEntry.Scale -> "Escala" to MaterialTheme.colorScheme.primary
        is AgendaEntry.Doxology -> "Doxologia" to MaterialTheme.colorScheme.secondary
        is AgendaEntry.Event -> entry.event.eventType.label to MaterialTheme.colorScheme.tertiary
        is AgendaEntry.AnnouncementEntry -> "Anúncio" to MaterialTheme.colorScheme.primary
        is AgendaEntry.Holiday -> "Feriado nacional" to MaterialTheme.colorScheme.error
    }
    val sourceType = when (entry) {
        is AgendaEntry.Scale -> entry.scale.sourceType
        is AgendaEntry.Doxology -> entry.doxology.sourceType
        is AgendaEntry.Event -> entry.event.sourceType
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .width(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (sourceType != null) {
                SourceBadge(sourceType, modifier = Modifier.padding(end = 8.dp))
            }
            if (entry !is AgendaEntry.Holiday && entry !is AgendaEntry.AnnouncementEntry) {
                Text(
                    entry.startTime.toDisplayString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
