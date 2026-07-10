package com.escalachurch.app.ui.stellacore

import com.escalachurch.app.domain.model.AccessLevel
import com.escalachurch.app.entitlements.EntitlementService

/**
 * Drops actions the current role can't perform outright - Fase 5 Section 4: role gating is a
 * hard filter, never a locked/blurred state (only plan gating gets that treatment, see
 * [StellaCoreFeatureResolver]).
 */
object StellaCorePermissionResolver {
    fun resolve(actions: List<StellaCoreAction>, currentRole: AccessLevel): List<StellaCoreAction> =
        actions.filter { it.requiresRole == null || it.requiresRole == currentRole }
}

/**
 * Resolves plan/feature gating for whatever actions survived [StellaCorePermissionResolver.resolve].
 * An action with no [StellaCoreAction.requiresFeature] always passes through. One that requires a
 * feature the church doesn't have is dropped unless it opted into [StellaCoreAction.lockedPreview],
 * in which case it's kept but flagged locked so the menu can show it with a discreet lock affordance
 * that opens the Premium Preview instead of running the action.
 */
object StellaCoreFeatureResolver {
    fun resolve(actions: List<StellaCoreAction>, entitlementService: EntitlementService): List<ResolvedStellaCoreAction> =
        actions.mapNotNull { action ->
            val feature = action.requiresFeature
            val hasFeature = feature == null || entitlementService.has(feature)
            when {
                hasFeature -> ResolvedStellaCoreAction(action, isLocked = false)
                action.lockedPreview -> ResolvedStellaCoreAction(action, isLocked = true)
                else -> null
            }
        }
}
