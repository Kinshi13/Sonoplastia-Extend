package com.escalachurch.app.church

import com.escalachurch.app.domain.model.Church

/** Fase 11.9A hotfix - explicit states for NavigationGate (see NavGraph.kt), replacing the old
 *  implicit "isReady + nullable activeChurch" pair the original bug hid in. */
sealed class BootstrapState {
    data object Loading : BootstrapState()
    data object NeedsChurchEntry : BootstrapState()
    data class HasActiveChurch(val church: Church) : BootstrapState()
    data class Error(val message: String) : BootstrapState()
}
