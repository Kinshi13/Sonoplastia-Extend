package com.escalachurch.app.ui.screens.home

import com.escalachurch.app.domain.util.friendlyErrorMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.data.repository.AnnouncementRepository
import com.escalachurch.app.data.repository.ChangeLogRepository
import com.escalachurch.app.data.repository.GeneralScaleRepository
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.data.repository.WorshipSongRepository
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.ChangeLogEntry
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.domain.model.UserClass
import com.escalachurch.app.domain.model.WorshipSong
import com.escalachurch.app.domain.util.NextItemResolver
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class HomeUiState(
    val churchName: String = "",
    val isAdmin: Boolean = false,
    val scales: List<ScaleItem> = emptyList(),
    val startIndex: Int? = null,
    val myClasses: Set<UserClass> = emptySet(),
    val pinnedAnnouncement: Announcement? = null,
    val todaysRecommendation: WorshipSong? = null,
    val isLoading: Boolean = true,
    // Bloco 18 - a failure in ONE source (e.g. Worship) never blanks the whole Home: each source
    // below is caught independently before the combine, so this only ever reflects the schedule
    // feed specifically failing - the one piece Home cannot render anything meaningful without.
    val hasLoadError: Boolean = false
) {
    val nextScale: ScaleItem? get() = scales.getOrNull(startIndex ?: 0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val scaleRepository: ScaleRepository,
    private val generalScaleRepository: GeneralScaleRepository,
    private val userProfileRepository: UserProfileRepository,
    private val changeLogRepository: ChangeLogRepository,
    private val adminSession: AdminSession,
    private val settingsRepository: com.escalachurch.app.data.repository.SettingsRepository,
    private val announcementRepository: AnnouncementRepository,
    private val worshipSongRepository: WorshipSongRepository,
    private val activeChurchManager: ActiveChurchManager
) : ViewModel() {

    /** Ticks every minute so the "next scale" recalculates automatically as time passes midnight. */
    private val clockTick = MutableStateFlow(LocalDateTime.now())

    /** Bumped by [retry]/[refresh] - re-subscribes the whole combine below from scratch, which
     *  re-runs each repository's underlying fetch instead of staying stuck on whatever failed. */
    private val retryTrigger = MutableStateFlow(0)

    private val isRefreshingState = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = isRefreshingState

    // Bloco 18 - each optional source is caught on its own, right at the source, so a failure in
    // Worship/Announcements never propagates into the core schedule combine below. Each falls back
    // to "nothing to show" (null/empty), not an error state - Home degrades a section, not itself.
    private val pinnedAnnouncementFlow: Flow<Announcement?> = announcementRepository.observeActive()
        .map { it.firstOrNull { a -> a.isPinned } }
        .catch { emit(null) }

    private val todaysRecommendationFlow: Flow<WorshipSong?> = worshipSongRepository.observePublished()
        .map { todaysRecommendationFrom(it, LocalDate.now()) }
        .catch { emit(null) }

    private val churchNameFlow: Flow<String> = activeChurchManager.activeChurch
        .map { it?.name.orEmpty() }
        .catch { emit("") }

    val uiState: StateFlow<HomeUiState> = retryTrigger.flatMapLatest {
        val core: Flow<CoreHomeData?> = combine(
            generalScaleRepository.observeOfficial(),
            clockTick,
            adminSession.isUnlocked,
            userProfileRepository.profileFlow,
            settingsRepository.settingsFlow
        ) { scales, now, isAdmin, profile, _ ->
            val sorted = NextItemResolver.sortedByDateTime(scales) { LocalDateTime.of(it.date, it.startTime) }
            val index = NextItemResolver.resolveStartIndex(sorted, { LocalDateTime.of(it.date, it.startTime) }, now)
            CoreHomeData(sorted, index, isAdmin, profile.selectedClasses) as CoreHomeData?
        }.catch { emit(null) } // null = the one failure that actually degrades the whole screen

        combine(core, pinnedAnnouncementFlow, todaysRecommendationFlow, churchNameFlow) { c, pinned, rec, churchName ->
            if (c == null) {
                HomeUiState(isLoading = false, hasLoadError = true, churchName = churchName)
            } else {
                HomeUiState(
                    churchName = churchName,
                    isAdmin = c.isAdmin,
                    scales = c.scales,
                    startIndex = c.startIndex,
                    myClasses = c.myClasses,
                    pinnedAnnouncement = pinned,
                    todaysRecommendation = rec,
                    isLoading = false
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private data class CoreHomeData(
        val scales: List<ScaleItem>,
        val startIndex: Int?,
        val isAdmin: Boolean,
        val myClasses: Set<UserClass>
    )

    fun retry() { retryTrigger.value++ }

    /** Bloco 20 - pull-to-refresh. Re-triggers every source's underlying fetch (same mechanism as
     *  [retry]) without touching the NavGraph/back stack - this is purely a data reload. [isRefreshing]
     *  clears itself shortly after, since the underlying flows don't expose a "finished" signal of
     *  their own; long enough for the pull-to-refresh spinner to read as a real action, never stuck. */
    fun refresh() {
        viewModelScope.launch {
            isRefreshingState.value = true
            retryTrigger.value++
            delay(600)
            isRefreshingState.value = false
        }
    }

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
                        flowOf(emptyList<ChangeLogEntry>())
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
