package com.escalachurch.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.data.preferences.AnnouncementSpotlightStore
import com.escalachurch.app.data.repository.AnnouncementRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.UserClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpotlightUiState(
    val queue: List<Announcement> = emptyList(),
    val currentIndex: Int = 0
) {
    val total: Int get() = queue.size
    val current: Announcement? get() = queue.getOrNull(currentIndex)
    val isVisible: Boolean get() = current != null
}

/**
 * Fase 11.9B Entrega 3 Bloco 4 - AnnouncementSpotlight's data layer. Recomputes the eligible queue
 * whenever the active church, the announcement feed, or the user's classes change - so switching
 * churches always resolves a fresh (never mixed) queue, per Bloco 23.
 *
 * Eligibility (honestly scoped to what the schema actually has - see AnnouncementSpotlightStore's
 * doc for what's missing): active, not yet confirmed for its current updatedAt, and either
 * directed at everyone (no affectedClasses) or at one of the viewer's own classes. There is no
 * priority/urgency column in `announcements` today, so "importante"/"urgente" cannot be evaluated -
 * ordering instead uses isPinned then most-recently-updated first, and no priority badge is shown
 * (see AnnouncementSpotlight.kt).
 */
class AnnouncementSpotlightViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val spotlightStore: AnnouncementSpotlightStore,
    private val activeChurchManager: ActiveChurchManager,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpotlightUiState())
    val uiState: StateFlow<SpotlightUiState> = _uiState

    /** "Agora não" only lasts for this ViewModel's lifetime (effectively this app session) - a
     *  fresh cold start recomputes without it, so the announcement can resurface later. */
    private val snoozedIds = mutableSetOf<String>()
    private var currentChurchId: String? = null

    init {
        viewModelScope.launch {
            combine(
                activeChurchManager.activeChurchId,
                announcementRepository.observeActive(),
                userProfileRepository.profileFlow
            ) { churchId, announcements, profile -> Triple(churchId, announcements, profile) }
                .collect { (churchId, announcements, profile) ->
                    currentChurchId = churchId
                    val confirmed = spotlightStore.confirmedUpdatedAt(churchId)
                    val eligible = eligibleSpotlightQueue(announcements, profile.selectedClasses, confirmed, snoozedIds)
                    _uiState.value = SpotlightUiState(queue = eligible, currentIndex = 0)
                }
        }
    }

    /** "Agora não" - dismiss for this session, may reappear on a later cold start. */
    fun snoozeCurrent() {
        _uiState.value.current?.let { snoozedIds.add(it.id) }
        advance()
    }

    /** "Marcar como visto" - persists so it won't resurface unless the announcement changes again. */
    fun confirmCurrent() {
        val announcement = _uiState.value.current ?: return
        val churchId = currentChurchId ?: return
        viewModelScope.launch { spotlightStore.markConfirmed(churchId, announcement.id, announcement.updatedAt) }
        advance()
    }

    /** "Ver anúncio" - counts as confirming it too (the user has now actually seen the content). */
    fun viewCurrent(): Announcement? {
        val announcement = _uiState.value.current
        confirmCurrent()
        return announcement
    }

    private fun advance() {
        _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
    }
}

/**
 * Pure - no Android/network dependency, so this is directly unit-tested (see
 * AnnouncementSpotlightQueueTest) instead of only provable by running the real app. This is the
 * exact rule set Bloco 4/23 describe: not snoozed this session, directed at the viewer (empty
 * affectedClasses means "everyone"), and not already confirmed for its current updatedAt - ordered
 * pinned-first then most-recently-updated, capped at 3.
 */
internal fun eligibleSpotlightQueue(
    announcements: List<Announcement>,
    myClasses: Set<UserClass>,
    confirmedUpdatedAt: Map<String, Long>,
    snoozedIds: Set<String>
): List<Announcement> = announcements
    .filter { it.id !in snoozedIds }
    .filter { it.affectedClasses.isEmpty() || it.affectedClasses.any { cls -> cls in myClasses } }
    .filter { val confirmedAt = confirmedUpdatedAt[it.id]; confirmedAt == null || confirmedAt < it.updatedAt }
    .sortedWith(compareByDescending<Announcement> { it.isPinned }.thenByDescending { it.updatedAt })
    .take(3)
