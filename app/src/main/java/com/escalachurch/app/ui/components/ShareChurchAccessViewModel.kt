package com.escalachurch.app.ui.components

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.BuildConfig
import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.domain.model.Church
import com.escalachurch.app.share.buildChurchShareMessage
import com.escalachurch.app.share.buildPublicChurchUrl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ShareChurchAccessUiState(
    val churchName: String = "",
    /** Public code, shown to the user - same value as [churchSlug] (Church.slug doc: "código
     *  público == slug hoje, sem coluna separada"), kept as its own field so every consumer reads
     *  a name that says what it is instead of overloading "slug". */
    val churchCode: String = "",
    val churchSlug: String = "",
    val publicUrl: String = "",
    val shareMessage: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Fase 11.10 (correção) - "Compartilhar acesso da igreja"'s data layer. Only reads
 * [ActiveChurchManager] (never writes to it - "sempre a igreja ativa, nunca outra") so switching
 * churches while the sheet happens to be open always resolves the currently active one.
 *
 * Root cause of the original bug: the Bottom Sheet built `message`/`link` itself by calling
 * separate ViewModel/builder functions inline on every recomposition, instead of the ViewModel
 * producing one finished [ShareChurchAccessUiState] with everything already resolved and
 * validated. All of link/code/message now come from the exact same [uiState] emission - see
 * [buildShareChurchAccessUiState], the pure function this just wires up to the live church flow.
 */
class ShareChurchAccessViewModel(
    activeChurchManager: ActiveChurchManager
) : ViewModel() {

    val uiState: StateFlow<ShareChurchAccessUiState> = activeChurchManager.activeChurch
        .map { church ->
            val state = buildShareChurchAccessUiState(church)
            if (BuildConfig.DEBUG) {
                Log.d(
                    "ShareChurchAccess",
                    "churchName=${state.churchName} churchCode=${state.churchCode} " +
                        "churchSlug=${state.churchSlug} publicUrl=${state.publicUrl} " +
                        "shareMessage.length=${state.shareMessage.length}"
                )
            }
            state
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShareChurchAccessUiState(isLoading = true))
}

/**
 * Fase 11.10 (correção) - pure resolution: active church -> full, validated
 * [ShareChurchAccessUiState]. No Android/network dependency, so this is directly unit-tested (see
 * ShareChurchAccessViewModelTest) instead of only provable by running the real app.
 *
 * Two distinct failure states, per this correction's own spec:
 *  - no [Church.slug] at all -> "verifique o cadastro da igreja" (the church record itself is
 *    incomplete).
 *  - slug present but [com.escalachurch.app.share.PUBLIC_WEB_BASE_URL] isn't configured ->
 *    "endereço público do site não configurado" (an environment/deploy problem, not a data one).
 * Neither ever produces a partial/relative URL - [ShareChurchAccessUiState.publicUrl] is either a
 * real absolute link or blank, never something in between.
 */
internal fun buildShareChurchAccessUiState(
    church: Church?,
    baseUrl: String = com.escalachurch.app.share.PUBLIC_WEB_BASE_URL
): ShareChurchAccessUiState {
    val name = church?.name.orEmpty()
    val slug = church?.slug.orEmpty()

    if (slug.isBlank()) {
        return ShareChurchAccessUiState(
            churchName = name,
            isLoading = false,
            errorMessage = "Não foi possível gerar o link público desta igreja. Verifique o cadastro da igreja."
        )
    }

    val publicUrl = buildPublicChurchUrl(slug, baseUrl)
    if (publicUrl == null) {
        return ShareChurchAccessUiState(
            churchName = name,
            churchCode = slug,
            churchSlug = slug,
            isLoading = false,
            errorMessage = "Endereço público do site não configurado."
        )
    }

    return ShareChurchAccessUiState(
        churchName = name,
        churchCode = slug,
        churchSlug = slug,
        publicUrl = publicUrl,
        shareMessage = buildChurchShareMessage(name, slug, publicUrl),
        isLoading = false,
        errorMessage = null
    )
}
