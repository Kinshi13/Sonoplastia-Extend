package com.escalachurch.app.ui.screens.generalscale

import com.escalachurch.app.domain.util.friendlyErrorMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.GeneralScaleRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.UserClass
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Ordering bucket for Escala Geral: Sábado first, then Quarta, then Domingo, then everything else. */
private fun dayRank(date: LocalDate): Int = when (date.dayOfWeek) {
    DayOfWeek.SATURDAY -> 0
    DayOfWeek.WEDNESDAY -> 1
    DayOfWeek.SUNDAY -> 2
    else -> 3
}

data class GeneralScaleUiState(
    val month: YearMonth = YearMonth.now(),
    val scales: List<ScaleItem> = emptyList(),
    val isAdmin: Boolean = false,
    val myClasses: Set<UserClass> = emptySet(),
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false
)

class GeneralScaleViewModel(
    private val generalScaleRepository: GeneralScaleRepository,
    private val userProfileRepository: UserProfileRepository,
    private val adminSession: AdminSession
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val onlyMyClasses = MutableStateFlow(false)
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<GeneralScaleUiState> = retryTrigger.flatMapLatest {
        combine(
            month,
            generalScaleRepository.observeOfficial(),
            adminSession.isUnlocked,
            userProfileRepository.profileFlow,
            onlyMyClasses
        ) { m, all, isAdmin, profile, filterMine ->
            val inMonth = all.filter { YearMonth.from(it.date) == m }
            val filtered = if (filterMine && profile.selectedClasses.isNotEmpty()) {
                inMonth.filter { it.assignedRolesByClass().keys.any { cls -> cls in profile.selectedClasses } }
            } else inMonth
            val ordered = filtered.sortedWith(compareBy({ dayRank(it.date) }, { it.date }, { it.startTime }))
            GeneralScaleUiState(month = m, scales = ordered, isAdmin = isAdmin, myClasses = profile.selectedClasses, isLoading = false)
        }.catch { emit(GeneralScaleUiState(month = month.value, isLoading = false, hasLoadError = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeneralScaleUiState())

    fun retry() { retryTrigger.value++ }

    val onlyMyClassesFlow: StateFlow<Boolean> = onlyMyClasses

    fun setMonth(newMonth: YearMonth) { month.value = newMonth }

    fun setOnlyMyClasses(value: Boolean) { onlyMyClasses.value = value }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun dismissError() { _errorMessage.value = null }

    fun save(item: ScaleItem, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { generalScaleRepository.saveOfficial(item) }
                .onSuccess { onSaved() }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao salvar a escala.") }
        }
    }

    fun delete(item: ScaleItem) {
        viewModelScope.launch {
            runCatching { generalScaleRepository.deleteOfficial(item) }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao excluir a escala.") }
        }
    }

    fun duplicateTo(item: ScaleItem, date: LocalDate) {
        viewModelScope.launch {
            runCatching { generalScaleRepository.duplicateTo(item, date) }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao duplicar a escala.") }
        }
    }

    fun createExtraDays(template: ScaleItem, dates: List<LocalDate>) {
        viewModelScope.launch {
            runCatching { generalScaleRepository.createExtraDays(template, dates) }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao criar os dias extras.") }
        }
    }
}
