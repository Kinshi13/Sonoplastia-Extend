package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.entitlements.FeatureKey
import com.escalachurch.app.entitlements.PlanCode
import com.escalachurch.app.entitlements.PlanLimits
import com.escalachurch.app.entitlements.SubscriptionStatus
import com.escalachurch.app.entitlements.SubscriptionSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the row shape returned by the `get_church_subscription` RPC (migration 012), not the
 * `subscriptions` table directly - no plan_id, no id, no source, no stripe_* columns. See
 * PlanRepository.observeSubscription().
 */
@Serializable
data class SubscriptionSummaryDto(
    @SerialName("church_id") val churchId: String? = null,
    @SerialName("plan_code") val planCode: String = "FREE",
    @SerialName("plan_name") val planName: String = "",
    @SerialName("subscription_status") val subscriptionStatus: String = "FREE",
    @SerialName("started_at") val startedAt: Long = 0L,
    @SerialName("expires_at") val expiresAt: Long? = null,
    @SerialName("trial_ends_at") val trialEndsAt: Long? = null,
    @SerialName("grace_period_ends_at") val gracePeriodEndsAt: Long? = null,
    val features: List<String> = emptyList(),
    val limits: PlanLimitsDto = PlanLimitsDto(),
    @SerialName("updated_at") val updatedAt: Long = 0L
)

fun SubscriptionSummaryDto.toSubscriptionSummary(): SubscriptionSummary? {
    return SubscriptionSummary(
        churchId = churchId ?: return null,
        planCode = PlanCode.fromCode(planCode),
        planName = planName,
        status = SubscriptionStatus.fromCode(subscriptionStatus),
        startedAt = startedAt,
        expiresAt = expiresAt,
        trialEndsAt = trialEndsAt,
        gracePeriodEndsAt = gracePeriodEndsAt,
        features = features.mapNotNull { name -> runCatching { FeatureKey.valueOf(name) }.getOrNull() }.toSet(),
        limits = PlanLimits(
            maxAdmins = limits.maxAdmins,
            maxPersonalEvents = limits.maxPersonalEvents,
            maxPersonalCards = limits.maxPersonalCards,
            historyMonths = limits.historyMonths,
            maxAnnouncements = limits.maxAnnouncements,
            maxMediaStorageMb = limits.maxMediaStorageMb,
            maxOrganizations = limits.maxOrganizations
        ),
        updatedAt = updatedAt
    )
}
