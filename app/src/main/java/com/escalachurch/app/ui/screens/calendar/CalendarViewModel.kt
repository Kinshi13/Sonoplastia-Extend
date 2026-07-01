package com.escalachurch.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.CustomEventRepository
import com.escalachurch.app.data.repository.DoxologyRepository
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.domain.holidays.BrazilianHolidays
import com.escalachurch.app.domain.model.AgendaEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.Year

class CalendarViewModel(
    scaleRepository: ScaleRepository,
    doxologyRepository: DoxologyRepository,
    customEventRepository: CustomEventRepository
) : ViewModel() {

    val entries: StateFlow<List<AgendaEntry>> = combine(
        scaleRepository.observeAll(),
        doxologyRepository.observeAll(),
        customEventRepository.observeAll()
    ) { scales, doxologies, events ->
        val holidays = (Year.now().value - 1..Year.now().value + 1)
            .flatMap { BrazilianHolidays.forYear(it) }
            .map { AgendaEntry.Holiday(it) }

        (scales.map { AgendaEntry.Scale(it) } +
            doxologies.map { AgendaEntry.Doxology(it) } +
            events.map { AgendaEntry.Event(it) } +
            holidays)
            .sortedWith(compareBy({ it.date }, { it.startTime }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun entriesFor(date: LocalDate, all: List<AgendaEntry>): List<AgendaEntry> =
        all.filter { it.date == date }
}
