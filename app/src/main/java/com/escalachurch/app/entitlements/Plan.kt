package com.escalachurch.app.entitlements

/** Numeric ceilings for a plan - a null field means unlimited. Never hardcoded in a screen;
 *  always read through EntitlementService.limit(...). */
data class PlanLimits(
    val maxAdmins: Int? = null,
    val maxPersonalEvents: Int? = null,
    val maxPersonalCards: Int? = null,
    val historyMonths: Int? = null,
    val maxAnnouncements: Int? = null,
    val maxMediaStorageMb: Int? = null,
    val maxOrganizations: Int? = null
)

data class Plan(
    val id: String,
    val code: PlanCode,
    val name: String,
    val description: String,
    val monthlyPriceCents: Int?,
    val yearlyPriceCents: Int?,
    val currency: String,
    val billingPeriod: String, // "recurring" | "one_time"
    val isActive: Boolean,
    val isPublic: Boolean,
    val sortOrder: Int,
    val features: Set<FeatureKey>,
    val limits: PlanLimits
)

data class Subscription(
    val id: String,
    val churchId: String,
    val planId: String,
    val status: SubscriptionStatus,
    val startedAt: Long,
    val expiresAt: Long?,
    val trialEndsAt: Long?,
    val gracePeriodEndsAt: Long?,
    val source: String,
    val updatedAt: Long
)
