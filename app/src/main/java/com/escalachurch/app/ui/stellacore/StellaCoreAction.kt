package com.escalachurch.app.ui.stellacore

import androidx.compose.ui.graphics.vector.ImageVector
import com.escalachurch.app.domain.model.AccessLevel
import com.escalachurch.app.entitlements.FeatureKey

/**
 * One action node in the Stella Core constellation menu. A screen contributes a small ordered
 * list of these (see StellaCoreConnector) instead of the menu owning any screen-specific logic -
 * Stella Core itself never knows what "Nova escala" means, only how to arrange and gate whatever
 * it's given.
 *
 * [requiresRole] hides the action entirely for the wrong role (e.g. an admin-only action is not
 * shown to a member at all - Fase 5 Section 4: "não mostrar ação que usuário não pode executar").
 * [requiresFeature] + [lockedPreview] are for the narrower "plan gate" case: when false the
 * action just doesn't appear (default - avoids "poluir o leque com cadeados"); set
 * [lockedPreview] = true only for the handful of actions where showing a locked state is itself
 * good marketing (opens the Premium Preview instead of running [onClick]).
 */
data class StellaCoreAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val requiresRole: AccessLevel? = null,
    val requiresFeature: FeatureKey? = null,
    val lockedPreview: Boolean = false,
    val onClick: () -> Unit
)

/** Result of resolving a raw action list against the current role + entitlements. */
data class ResolvedStellaCoreAction(
    val action: StellaCoreAction,
    val isLocked: Boolean
)
