package com.escalachurch.app.ui.screens.doxology

import com.escalachurch.app.domain.util.friendlyErrorMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.DoxologyRepository
import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.domain.util.NextItemResolver
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class DoxologyUiState(
    val items: List<DoxologyItem> = emptyList(),
    val startIndex: Int? = null,
    /** Id of the session currently in progress (has an end_time and "now" falls inside its
     *  range), if any - lets the card show an "Agora" badge regardless of which page is open. */
    val liveItemId: String? = null,
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val hasLoadError: Boolean = false
)

class DoxologyViewModel(
    private val repository: DoxologyRepository,
    private val adminSession: AdminSession
) : ViewModel() {

    private val clockTick = MutableStateFlow(LocalDateTime.now())
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<DoxologyUiState> = retryTrigger.flatMapLatest {
        combine(repository.observeAll(), clockTick, adminSession.isUnlocked) { items, now, isAdmin ->
            val sorted = NextItemResolver.sortedByDateTime(items) { LocalDateTime.of(it.date, it.startTime) }
            val startOf: (DoxologyItem) -> LocalDateTime = { LocalDateTime.of(it.date, it.startTime) }
            val endOf: (DoxologyItem) -> LocalDateTime? = { item -> item.endTime?.let { LocalDateTime.of(item.date, it) } }
            val liveIndex = NextItemResolver.resolveCurrentIndex(sorted, startOf, endOf, now)
            val index = liveIndex ?: NextItemResolver.resolveStartIndex(sorted, startOf, now)
            DoxologyUiState(
                items = sorted,
                startIndex = index,
                liveItemId = liveIndex?.let { sorted[it].id },
                isLoading = false,
                isAdmin = isAdmin
            )
        }.catch { emit(DoxologyUiState(isLoading = false, hasLoadError = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DoxologyUiState())

    fun retry() { retryTrigger.value++ }

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                clockTick.value = LocalDateTime.now()
            }
        }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun dismissError() { _errorMessage.value = null }

    fun save(item: DoxologyItem, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.save(item.copy(updatedAt = System.currentTimeMillis())) }
                .onSuccess { onSaved() }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao salvar a doxologia.") }
        }
    }

    fun delete(item: DoxologyItem) {
        viewModelScope.launch {
            runCatching { repository.deleteById(item.id) }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao excluir a doxologia.") }
        }
    }
}
