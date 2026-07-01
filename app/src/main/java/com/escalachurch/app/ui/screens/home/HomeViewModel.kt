package com.escalachurch.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.util.NextItemResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class HomeUiState(
    val scales: List<ScaleItem> = emptyList(),
    val startIndex: Int? = null,
    val isLoading: Boolean = true
)

class HomeViewModel(private val repository: ScaleRepository) : ViewModel() {

    /** Ticks every minute so the "next scale" recalculates automatically as time passes midnight. */
    private val clockTick = MutableStateFlow(LocalDateTime.now())

    val uiState: StateFlow<HomeUiState> = combine(repository.observeAll(), clockTick) { scales, now ->
        val sorted = NextItemResolver.sortedByDateTime(scales) { LocalDateTime.of(it.date, it.startTime) }
        val index = NextItemResolver.resolveStartIndex(sorted, { LocalDateTime.of(it.date, it.startTime) }, now)
        HomeUiState(scales = sorted, startIndex = index, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                clockTick.value = LocalDateTime.now()
            }
        }
    }

    fun save(item: ScaleItem, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.save(item.copy(updatedAt = System.currentTimeMillis()))
            onSaved()
        }
    }

    fun delete(item: ScaleItem) {
        viewModelScope.launch { repository.delete(item) }
    }
}
