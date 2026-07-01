package com.escalachurch.app.ui.screens.doxology

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.DoxologyRepository
import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.domain.util.NextItemResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class DoxologyUiState(
    val items: List<DoxologyItem> = emptyList(),
    val startIndex: Int? = null,
    val isLoading: Boolean = true
)

class DoxologyViewModel(private val repository: DoxologyRepository) : ViewModel() {

    private val clockTick = MutableStateFlow(LocalDateTime.now())

    val uiState: StateFlow<DoxologyUiState> = combine(repository.observeAll(), clockTick) { items, now ->
        val sorted = NextItemResolver.sortedByDateTime(items) { LocalDateTime.of(it.date, it.startTime) }
        val index = NextItemResolver.resolveStartIndex(sorted, { LocalDateTime.of(it.date, it.startTime) }, now)
        DoxologyUiState(items = sorted, startIndex = index, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DoxologyUiState())

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                clockTick.value = LocalDateTime.now()
            }
        }
    }

    fun save(item: DoxologyItem, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.save(item.copy(updatedAt = System.currentTimeMillis()))
            onSaved()
        }
    }

    fun delete(item: DoxologyItem) {
        viewModelScope.launch { repository.deleteById(item.id) }
    }
}
