package com.escalachurch.app.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.share.buildChurchLink
import com.escalachurch.app.share.buildChurchShareMessage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ShareChurchAccessUiState(
    val churchName: String = "",
    val churchSlug: String = "",
    val link: String = ""
) {
    /** Never empty church data reaches the sheet with a broken link. */
    val isReady: Boolean get() = churchSlug.isNotBlank()
}

/**
 * Fase 11.10 - "Compartilhar acesso da igreja"'s data layer. Only reads [ActiveChurchManager]
 * (never writes to it - Bloco: "sempre a igreja ativa, nunca outra") so switching churches while
 * the sheet happens to be open always resolves the currently active one, same as every other
 * screen that reads `activeChurch`.
 *
 * Deliberately does not fetch a "next scale" itself - the caller (Home/Escala Geral, which
 * already have that data loaded for their own screen) passes it straight into
 * [com.escalachurch.app.ui.components.ShareChurchAccessBottomSheet] instead of this ViewModel
 * re-querying it, avoiding a second copy of NextItemResolver's logic.
 */
class ShareChurchAccessViewModel(
    activeChurchManager: ActiveChurchManager,
    private val siteBaseUrl: String
) : ViewModel() {

    val uiState: StateFlow<ShareChurchAccessUiState> = activeChurchManager.activeChurch
        .map { church ->
            val slug = church?.slug.orEmpty()
            ShareChurchAccessUiState(
                churchName = church?.name.orEmpty(),
                churchSlug = slug,
                link = if (slug.isBlank()) "" else buildChurchLink(siteBaseUrl, slug)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShareChurchAccessUiState())

    fun buildMessage(nextScaleSummary: String?): String {
        val state = uiState.value
        return buildChurchShareMessage(state.churchName, state.churchSlug, state.link, nextScaleSummary)
    }
}
