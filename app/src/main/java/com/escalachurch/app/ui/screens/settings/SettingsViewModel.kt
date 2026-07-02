package com.escalachurch.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.domain.model.UserClass
import com.escalachurch.app.domain.model.UserProfile
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val profile: UserProfile = UserProfile(),
    val isAdmin: Boolean = false
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    val adminSession: AdminSession
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settingsFlow,
        userProfileRepository.profileFlow,
        adminSession.isUnlocked
    ) { s, p, admin -> SettingsUiState(s, p, admin) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = settings.value
            repository.update(transform(current))
        }
    }

    fun toggleClass(userClass: UserClass) {
        viewModelScope.launch {
            val current = uiState.value.profile.selectedClasses
            val updated = if (userClass in current) current - userClass else current + userClass
            userProfileRepository.updateClasses(updated)
        }
    }

    fun updateMyDisplayName(name: String) {
        viewModelScope.launch { userProfileRepository.updateName(name) }
    }
}
