package com.escalachurch.app.ui.screens.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.CustomEventRepository
import com.escalachurch.app.domain.model.CustomEvent
import com.escalachurch.app.domain.util.NextItemResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class ProgramUiState(
    val events: List<CustomEvent> = emptyList(),
    val startIndex: Int? = null,
    val isLoading: Boolean = true
)

class ProgramViewModel(private val repository: CustomEventRepository) : ViewModel() {

    /** Ticks every minute so "next program" recalculates automatically, same as Início/Doxologia. */
    private val clockTick = MutableStateFlow(LocalDateTime.now())

    val uiState: StateFlow<ProgramUiState> = combine(repository.observeAll(), clockTick) { events, now ->
        val sorted = NextItemResolver.sortedByDateTime(events) { LocalDateTime.of(it.date, it.startTime) }
        val index = NextItemResolver.resolveStartIndex(sorted, { LocalDateTime.of(it.date, it.startTime) }, now)
        ProgramUiState(events = sorted, startIndex = index, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgramUiState())

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                clockTick.value = LocalDateTime.now()
            }
        }
    }

    fun save(
        event: CustomEvent,
        additionalDates: List<LocalDate> = emptyList(),
        weeklyOccurrences: Int = 0,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveWithDuplicates(event, additionalDates, weeklyOccurrences)
            onSaved()
        }
    }

    fun delete(event: CustomEvent) {
        viewModelScope.launch { repository.deleteById(event.id) }
    }
}
