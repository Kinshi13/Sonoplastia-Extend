package com.escalachurch.app.ui.screens.announcements

import com.escalachurch.app.domain.util.friendlyErrorMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.AnnouncementRepository
import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.UserClass
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnnouncementsUiState(
    val announcements: List<Announcement> = emptyList(),
    val isAdmin: Boolean = false,
    val myClasses: Set<UserClass> = emptySet(),
    val lastSeenAt: Long = 0L,
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false
)

class AnnouncementViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val settingsRepository: SettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val adminSession: AdminSession
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<AnnouncementsUiState> = retryTrigger.flatMapLatest {
        combine(
            announcementRepository.observeActive(),
            adminSession.isUnlocked,
            userProfileRepository.profileFlow,
            settingsRepository.settingsFlow
        ) { announcements, isAdmin, profile, settings ->
            AnnouncementsUiState(
                announcements = announcements,
                isAdmin = isAdmin,
                myClasses = profile.selectedClasses,
                lastSeenAt = settings.lastSeenAnnouncementsAt,
                isLoading = false
            )
        }.catch { emit(AnnouncementsUiState(isLoading = false, hasLoadError = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnnouncementsUiState())

    fun retry() { retryTrigger.value++ }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun dismissError() { _errorMessage.value = null }

    fun save(item: Announcement) {
        viewModelScope.launch {
            runCatching { announcementRepository.save(item) }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao salvar o anúncio.") }
        }
    }

    suspend fun uploadMedia(context: android.content.Context, uri: android.net.Uri) =
        announcementRepository.uploadMedia(context, uri)

    fun delete(item: Announcement) {
        viewModelScope.launch {
            runCatching { announcementRepository.delete(item) }
                .onFailure { _errorMessage.value = friendlyErrorMessage(it, "Falha ao excluir o anúncio.") }
        }
    }

    /** Marks all currently-loaded announcements as seen (clears "Novo" badges and the top-level badge count). */
    fun markAllSeen() {
        viewModelScope.launch {
            val current = settingsRepository.settingsFlow.first()
            settingsRepository.update(current.copy(lastSeenAnnouncementsAt = System.currentTimeMillis()))
        }
    }
}
