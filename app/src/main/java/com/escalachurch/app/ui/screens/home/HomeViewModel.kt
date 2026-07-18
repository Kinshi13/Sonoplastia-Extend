package com.escalachurch.app.ui.screens.home

import com.escalachurch.app.domain.util.friendlyErrorMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.ChangeLogRepository
import com.escalachurch.app.data.repository.GeneralScaleRepository
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.domain.model.ChangeLogEntry
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.domain.model.UserClass
import com.escalachurch.app.domain.util.NextItemResolver
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

data class HomeUiState(
    val scales: List<ScaleItem> = emptyList(),
    val startIndex: Int? = null,
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val myClasses: Set<UserClass> = emptySet(),
    // Fase 11.9B Bloco 6 - distinguishes "the feed genuinely failed to load" from "loading" and
    // from "genuinely no scales", so the screen can show CelestialErrorState with a real retry
    // instead of an EmptyState that implies there's nothing to add.
    val hasLoadError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val scaleRepository: ScaleRepository,
    private val generalScaleRepository: GeneralScaleRepository,
    private val userProfileRepository: UserProfileRepository,
    private val changeLogRepository: ChangeLogRepository,
    private val adminSession: AdminSession,
    private val settingsRepository: com.escalachurch.app.data.repository.SettingsRepository
) : ViewModel() {

    /** Ticks every minute so the "next scale" recalculates automatically as time passes midnight. */
    private val clockTick = MutableStateFlow(LocalDateTime.now())

    /** Bumped by [retry] - re-subscribes the whole combine below from scratch, which re-runs each
     *  repository's underlying fetch (see ScaleRepository/observeTable) instead of staying stuck
     *  on whatever failed the first time. */
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = retryTrigger.flatMapLatest {
        combine(
            generalScaleRepository.observeOfficial(),
            clockTick,
            adminSession.isUnlocked,
            userProfileRepository.profileFlow,
            settingsRepository.settingsFlow
        ) { scales, now, isAdmin, profile, _ ->
            val sorted = NextItemResolver.sortedByDateTime(scales) { LocalDateTime.of(it.date, it.startTime) }
            val index = NextItemResolver.resolveStartIndex(sorted, { LocalDateTime.of(it.date, it.startTime) }, now)
            HomeUiState(
                scales = sorted,
                startIndex = index,
                isLoading = false,
                isAdmin = isAdmin,
                myClasses = profile.selectedClasses
            )
        }.catch { emit(HomeUiState(isLoading = false, hasLoadError = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun retry() { retryTrigger.value++ }

    private val _pendingNewsEntry = MutableStateFlow<ChangeLogEntry?>(null)
    val pendingNewsEntry: StateFlow<ChangeLogEntry?> = _pendingNewsEntry

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun dismissError() { _errorMessage.value = null }

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                clockTick.value = LocalDateTime.now()
            }
        }
        viewModelScope.launch {
            combine(settingsRepository.settingsFlow, userProfileRepository.profileFlow) { s, p -> s to p }
                .flatMapLatest { (s, p) ->
                    if (!s.showNewsPopupOnOpen) {
                        kotlinx.coroutines.flow.flowOf(emptyList<ChangeLogEntry>())
                    } else {
                        changeLogRepository.observeUnseen(p.selectedClasses, s.notifyOnlyMyClasses)
                    }
                }
                .collect { unseen ->
                    if (unseen.isNotEmpty() && _pendingNewsEntry.value == null) {
                        _pendingNewsEntry.value = unseen.first()
                    }
                }
        }
    }

    fun dismissNews(markSeen: Boolean) {
        val entry = _pendingNewsEntry.value ?: return
        _pendingNewsEntry.value = null
        if (markSeen) {
            viewModelScope.launch { changeLogRepository.markSeen(entry) }
        }
    }

    fun save(item: ScaleItem, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                if (item.sourceType == SourceType.OFFICIAL) {
                    generalScaleRepository.saveOfficial(item.copy(updatedAt = System.currentTimeMillis()))
                } else {
                    scaleRepository.save(item.copy(updatedAt = System.currentTimeMillis()))
                }
            }.onSuccess { onSaved() }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao salvar a escala.") }
        }
    }

    fun delete(item: ScaleItem) {
        viewModelScope.launch {
            runCatching { scaleRepository.delete(item) }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao excluir a escala.") }
        }
    }
}
