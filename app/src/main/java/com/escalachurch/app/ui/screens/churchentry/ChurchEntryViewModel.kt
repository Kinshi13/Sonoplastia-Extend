package com.escalachurch.app.ui.screens.churchentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.data.preferences.RecentChurchStore
import com.escalachurch.app.data.repository.ChurchRepository
import com.escalachurch.app.domain.model.Church
import com.escalachurch.app.domain.model.RecentChurch
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChurchEntryUiState(
    val code: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val recentChurches: List<RecentChurch> = emptyList(),
    val showAdminLogin: Boolean = false,
    val adminLoginError: String? = null,
    val adminLoginBusy: Boolean = false,
    val forgotPasswordMessage: String? = null
)

/** Fase 11.9A - backs ChurchEntryScreen: first-use code entry, "Continuar em" for recent
 *  churches, and the Admin login entry point (item 10 - integrated here, not only in Settings). */
class ChurchEntryViewModel(
    private val churchRepository: ChurchRepository,
    private val recentChurchStore: RecentChurchStore,
    private val activeChurchManager: ActiveChurchManager,
    val adminSession: AdminSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChurchEntryUiState())
    val uiState: StateFlow<ChurchEntryUiState> = _uiState

    init {
        recentChurchStore.recentChurchesFlow
            .onEach { list -> _uiState.update { it.copy(recentChurches = list) } }
            .launchIn(viewModelScope)
    }

    fun onCodeChange(value: String) {
        _uiState.update { it.copy(code = value, errorMessage = null) }
    }

    fun submitCode() {
        val code = _uiState.value.code.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val church = runCatching { churchRepository.findByCode(code) }.getOrNull()
            when {
                church == null -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = "Código não encontrado. Confira e tente novamente.")
                }
                !church.isActive -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = "Esta igreja ainda não está ativa.")
                }
                else -> {
                    activeChurchManager.setActiveChurch(church)
                    _uiState.update { it.copy(isSubmitting = false, code = "") }
                }
            }
        }
    }

    /** "Continuar em [nome]" - re-checks the church server-side (name/active status may have
     *  changed since it was recorded), but falls back to the cached RecentChurch entry if that
     *  check itself fails (offline), rather than blocking a fast path that used to work. */
    fun continueWith(recent: RecentChurch) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = runCatching { churchRepository.findById(recent.churchId) }
            val church = result.getOrNull() ?: if (result.isFailure) {
                Church(id = recent.churchId, slug = recent.churchCode, name = recent.churchName, isActive = true)
            } else null
            if (church == null || !church.isActive) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = "Esta igreja não está mais disponível.") }
            } else {
                activeChurchManager.setActiveChurch(church)
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun setFavorite(churchId: String, favorite: Boolean) {
        viewModelScope.launch { recentChurchStore.setFavorite(churchId, favorite) }
    }

    fun removeRecent(churchId: String) {
        viewModelScope.launch { recentChurchStore.remove(churchId) }
    }

    fun openAdminLogin() = _uiState.update { it.copy(showAdminLogin = true, adminLoginError = null, forgotPasswordMessage = null) }
    fun dismissAdminLogin() = _uiState.update { it.copy(showAdminLogin = false, adminLoginError = null, forgotPasswordMessage = null) }

    fun adminSignIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(adminLoginBusy = true, adminLoginError = null) }
            val result = adminSession.signIn(email, password)
            if (result.isSuccess) {
                _uiState.update { it.copy(showAdminLogin = false, adminLoginError = null, adminLoginBusy = false) }
            } else {
                _uiState.update { it.copy(adminLoginError = "Não foi possível entrar. Confira o e-mail e a senha.", adminLoginBusy = false) }
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(adminLoginBusy = true) }
            val result = adminSession.resetPassword(email)
            _uiState.update {
                it.copy(
                    adminLoginBusy = false,
                    forgotPasswordMessage = if (result.isSuccess) {
                        "Se este e-mail estiver cadastrado, enviamos um link de recuperação."
                    } else {
                        "Não foi possível enviar o link agora. Tente novamente."
                    }
                )
            }
        }
    }
}
