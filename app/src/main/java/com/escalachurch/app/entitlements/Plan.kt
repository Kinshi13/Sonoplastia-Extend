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

/**
 * The minimal, pre-joined shape returned by the `get_church_subscription` RPC (migration 012) -
 * plan_code/name/features/limits come straight off this row, never resolved separately by
 * plan_id. Never carries plan_id, stripe_customer_id or stripe_subscription_id - the client has
 * no business depending on those, and the RPC deliberately doesn't expose them.
 */
data class SubscriptionSummary(
    val churchId: String,
    val planCode: PlanCode,
    val planName: String,
    val status: SubscriptionStatus,
    val startedAt: Long,
    val expiresAt: Long?,
    val trialEndsAt: Long?,
    val gracePeriodEndsAt: Long?,
    val features: Set<FeatureKey>,
    val limits: PlanLimits,
    val updatedAt: Long
) {
    /** Status alone isn't enough - a row can still say ACTIVE/TRIAL/GRACE_PERIOD after its own
     *  cutoff has passed if the writer (Stripe webhook, manual grant) hasn't caught up yet. Each
     *  status is checked against its own relevant cutoff, not a single generic "expires_at". */
    fun isCurrentlyValid(now: Long = System.currentTimeMillis()): Boolean {
        if (!status.grantsAccess) return false
        val cutoff = when (status) {
            SubscriptionStatus.TRIAL -> trialEndsAt
            SubscriptionStatus.GRACE_PERIOD -> gracePeriodEndsAt
            else -> expiresAt
        }
        return cutoff == null || now < cutoff
    }
}
