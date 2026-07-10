package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.entitlements.Subscription
import com.escalachurch.app.entitlements.SubscriptionStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionDto(
    val id: String? = null,
    @SerialName("church_id") val churchId: String = BuildConfig.CHURCH_ID,
    @SerialName("plan_id") val planId: String = "",
    val status: String = "FREE",
    @SerialName("started_at") val startedAt: Long = 0L,
    @SerialName("expires_at") val expiresAt: Long? = null,
    @SerialName("trial_ends_at") val trialEndsAt: Long? = null,
    @SerialName("grace_period_ends_at") val gracePeriodEndsAt: Long? = null,
    val source: String = "manual",
    @SerialName("updated_at") val updatedAt: Long = 0L
)

fun SubscriptionDto.toSubscription(): Subscription? {
    return Subscription(
        id = id ?: return null,
        churchId = churchId,
        planId = planId,
        status = SubscriptionStatus.fromCode(status),
        startedAt = startedAt,
        expiresAt = expiresAt,
        trialEndsAt = trialEndsAt,
        gracePeriodEndsAt = gracePeriodEndsAt,
        source = source,
        updatedAt = updatedAt
    )
}
