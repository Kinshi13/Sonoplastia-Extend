package com.escalachurch.app.ui.screens.generalscale

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
import kotlinx.coroutines.flow.combine
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
    val myClasses: Set<UserClass> = emptySet()
)

class GeneralScaleViewModel(
    private val generalScaleRepository: GeneralScaleRepository,
    private val userProfileRepository: UserProfileRepository,
    private val adminSession: AdminSession
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val onlyMyClasses = MutableStateFlow(false)

    val uiState: StateFlow<GeneralScaleUiState> = combine(
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
        GeneralScaleUiState(month = m, scales = ordered, isAdmin = isAdmin, myClasses = profile.selectedClasses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeneralScaleUiState())

    val onlyMyClassesFlow: StateFlow<Boolean> = onlyMyClasses

    fun setMonth(newMonth: YearMonth) { month.value = newMonth }

    fun setOnlyMyClasses(value: Boolean) { onlyMyClasses.value = value }

    fun save(item: ScaleItem, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            generalScaleRepository.saveOfficial(item)
            onSaved()
        }
    }

    fun delete(item: ScaleItem) {
        viewModelScope.launch { generalScaleRepository.deleteOfficial(item) }
    }

    fun duplicateTo(item: ScaleItem, date: LocalDate) {
        viewModelScope.launch { generalScaleRepository.duplicateTo(item, date) }
    }

    fun createExtraDays(template: ScaleItem, dates: List<LocalDate>) {
        viewModelScope.launch { generalScaleRepository.createExtraDays(template, dates) }
    }
}
