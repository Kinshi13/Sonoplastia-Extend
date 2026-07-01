package com.escalachurch.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.domain.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = settings.value
            repository.update(transform(current))
        }
    }
}
