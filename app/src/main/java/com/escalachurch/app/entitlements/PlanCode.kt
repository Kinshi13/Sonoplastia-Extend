package com.escalachurch.app.entitlements

/** Mirrors the `code` column of the `plans` table (Fase 3 catalog). */
enum class PlanCode {
    FREE,
    ESSENTIAL,
    PRO,
    ORGANIZATION,
    FOUNDER;

    companion object {
        fun fromCode(code: String): PlanCode = entries.firstOrNull { it.name == code } ?: FREE
    }
}

/** Mirrors `subscriptions.status`. */
enum class SubscriptionStatus {
    FREE,
    TRIAL,
    ACTIVE,
    PAST_DUE,
    GRACE_PERIOD,
    CANCELED,
    EXPIRED;

    /** Whether the plan's features should currently be granted - PAST_DUE/GRACE_PERIOD still
     *  count as "keep access" (that's the point of a grace period); CANCELED/EXPIRED don't. */
    val grantsAccess: Boolean
        get() = this == FREE || this == TRIAL || this == ACTIVE || this == PAST_DUE || this == GRACE_PERIOD

    companion object {
        fun fromCode(code: String): SubscriptionStatus = entries.firstOrNull { it.name == code } ?: FREE
    }
}

/**
 * One capability an entitlement can gate. Every screen checks these instead of scattering its
 * own `isPremium`/`hasPro`/`canUseX` booleans - see EntitlementService.
 */
enum class FeatureKey {
    VIEW_OFFICIAL_SCALE,
    VIEW_DOXOLOGY,
    VIEW_ANNOUNCEMENTS,
    VIEW_CALENDAR,
    CLASS_HIGHLIGHTS,
    PERSONAL_EVENTS,
    PERSONAL_CARDS,
    EXTENDED_HISTORY,
    ADVANCED_ADMIN,
    MULTI_ADMIN,
    ADVANCED_NOTIFICATIONS,
    CUSTOM_FONTS,
    PREMIUM_FONTS,
    CUSTOM_THEMES,
    PREMIUM_THEMES,
    EXPORT,
    REPORTS,
    ORGANIZATION_BRANDING,
    ADVANCED_MEDIA,
    PRIORITY_SYNC,
    AUTOMATIONS
}
