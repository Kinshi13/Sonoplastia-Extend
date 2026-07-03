package com.escalachurch.app.ui.screens.sonoplastia

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.SonoplastiaFileRepository
import com.escalachurch.app.domain.model.SharedFile
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SonoplastiaUiState(
    val files: List<SharedFile> = emptyList(),
    val isAdmin: Boolean = false
)

/**
 * Remote file sharing (phone <-> PC): the admin uploads a PPT/PDF/photo/video here and it's
 * immediately available to open/download from any other device signed into the same church,
 * without needing Bluetooth/Wi-Fi pairing between the two machines.
 */
class SonoplastiaViewModel(
    private val repository: SonoplastiaFileRepository,
    private val adminSession: AdminSession
) : ViewModel() {

    val uiState: StateFlow<SonoplastiaUiState> = combine(
        repository.observeAll(),
        adminSession.isUnlocked
    ) { files, isAdmin ->
        SonoplastiaUiState(files = files, isAdmin = isAdmin)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SonoplastiaUiState())

    suspend fun upload(context: Context, uri: Uri): SharedFile = repository.upload(context, uri)

    fun delete(file: SharedFile) {
        viewModelScope.launch { repository.delete(file) }
    }
}
