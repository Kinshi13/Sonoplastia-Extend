package com.escalachurch.app.ui.screens.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.CustomEventRepository
import com.escalachurch.app.domain.model.CustomEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ProgramViewModel(private val repository: CustomEventRepository) : ViewModel() {

    val events: StateFlow<List<CustomEvent>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
