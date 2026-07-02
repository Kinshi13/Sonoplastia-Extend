package com.escalachurch.app.ui.screens.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.AnnouncementRepository
import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.UserClass
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnnouncementsUiState(
    val announcements: List<Announcement> = emptyList(),
    val isAdmin: Boolean = false,
    val myClasses: Set<UserClass> = emptySet(),
    val lastSeenAt: Long = 0L
)

class AnnouncementViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val settingsRepository: SettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val adminSession: AdminSession
) : ViewModel() {

    val uiState: StateFlow<AnnouncementsUiState> = combine(
        announcementRepository.observeActive(),
        adminSession.isUnlocked,
        userProfileRepository.profileFlow,
        settingsRepository.settingsFlow
    ) { announcements, isAdmin, profile, settings ->
        AnnouncementsUiState(
            announcements = announcements,
            isAdmin = isAdmin,
            myClasses = profile.selectedClasses,
            lastSeenAt = settings.lastSeenAnnouncementsAt
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnnouncementsUiState())

    fun save(item: Announcement) {
        viewModelScope.launch { announcementRepository.save(item) }
    }

    fun delete(item: Announcement) {
        viewModelScope.launch { announcementRepository.delete(item) }
    }

    /** Marks all currently-loaded announcements as seen (clears "Novo" badges and the top-level badge count). */
    fun markAllSeen() {
        viewModelScope.launch {
            val current = settingsRepository.settingsFlow.first()
            settingsRepository.update(current.copy(lastSeenAnnouncementsAt = System.currentTimeMillis()))
        }
    }
}
