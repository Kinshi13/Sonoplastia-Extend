package com.escalachurch.app.entitlements

import com.escalachurch.app.data.repository.PlanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** The hardcoded floor every church always gets, even fully offline with no cache at all - see
 *  Fase 3 Section 6 (Free-tier guarantees) and Section 11 (never fail closed below FREE). */
private val FREE_FEATURES = setOf(
    FeatureKey.VIEW_OFFICIAL_SCALE,
    FeatureKey.VIEW_DOXOLOGY,
    FeatureKey.VIEW_ANNOUNCEMENTS,
    FeatureKey.VIEW_CALENDAR,
    FeatureKey.CLASS_HIGHLIGHTS,
    FeatureKey.PERSONAL_EVENTS,
    FeatureKey.PERSONAL_CARDS
)
private val FREE_LIMITS = PlanLimits(
    maxAdmins = 1, maxPersonalEvents = 10, maxPersonalCards = 5,
    historyMonths = 1, maxAnnouncements = null, maxMediaStorageMb = 100, maxOrganizations = 1
)

/** What every screen actually reads - resolved once here instead of each screen re-deriving it
 *  from a raw Plan + Subscription pair. */
data class Entitlements(
    val planCode: PlanCode,
    val planName: String,
    val status: SubscriptionStatus,
    val features: Set<FeatureKey>,
    val limits: PlanLimits,
    val isFromCache: Boolean
) {
    fun has(feature: FeatureKey): Boolean = feature in features
}

private val FREE_FLOOR = Entitlements(
    planCode = PlanCode.FREE,
    planName = "Free",
    status = SubscriptionStatus.FREE,
    features = FREE_FEATURES,
    limits = FREE_LIMITS,
    isFromCache = false
)

/**
 * Single source of truth for "what can this church do right now." Reads this church's own
 * subscription summary (plan/status/expiration/features/limits, pre-joined server-side by
 * `get_church_subscription`); while offline (no subscription flow emission), falls back to the
 * last cached entitlement if it's still fresh, and to the hardcoded FREE floor otherwise - never
 * grants indefinite premium access purely from a stale cache (Fase 3 Section 11).
 */
class EntitlementService(
    private val planRepository: PlanRepository,
    private val cacheStore: EntitlementCacheStore,
    scope: CoroutineScope
) {
    // Hotfix (Fase 11.9): tracked independently of the subscription fetch's own success/failure,
    // so a cache lookup during a *first-ever* offline resolution still knows which church it's for
    // - see resolveFromCache() and EntitlementCacheStore.
    private var currentChurchId: String? = null

    init {
        planRepository.observeChurchId().onEach { currentChurchId = it }.launchIn(scope)
    }

    // Hotfix (Fase 11.9): the RPC already returns plan_code/features/limits pre-joined to the
    // subscription row - no separate `plans` lookup by plan_id needed or wanted here anymore.
    // Status and expiration are both validated explicitly: a row can say ACTIVE/TRIAL/GRACE_PERIOD
    // and still be past its own cutoff if the writer (Stripe webhook, manual grant) hasn't caught
    // up yet - isCurrentlyValid() checks status.grantsAccess AND the cutoff that status implies
    // (trial_ends_at / grace_period_ends_at / expires_at) before any feature is granted.
    val entitlements: StateFlow<Entitlements> = planRepository.observeSubscription()
        .map { subscription ->
            if (subscription == null || !subscription.isCurrentlyValid()) {
                FREE_FLOOR
            } else {
                Entitlements(
                    planCode = subscription.planCode,
                    planName = subscription.planName,
                    status = subscription.status,
                    features = subscription.features,
                    limits = subscription.limits,
                    isFromCache = false
                )
            }
        }
        .catch { emit(resolveFromCache()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), FREE_FLOOR)

    init {
        // Keep the cache warm any time we resolve a real (non-cache, non-error) entitlement, so a
        // later offline session has something better than FREE_FLOOR to fall back to.
        entitlements.onEach { current ->
            val churchId = currentChurchId
            if (!current.isFromCache && churchId != null) {
                cacheStore.save(churchId, current.planCode, current.status, current.features)
            }
        }.launchIn(scope)
    }

    private suspend fun resolveFromCache(): Entitlements {
        val cached = cacheStore.current() ?: return FREE_FLOOR
        if (!cached.isFresh()) return FREE_FLOOR
        // A cache from a different church (e.g. an admin who just logged into another church's
        // account) must never leak that church's plan into this one - treat it as no cache at all.
        if (currentChurchId != null && cached.churchId != currentChurchId) return FREE_FLOOR
        return Entitlements(
            planCode = cached.planCode,
            planName = cached.planCode.name,
            status = cached.status,
            features = cached.features,
            limits = FREE_LIMITS,
            isFromCache = true
        )
    }

    fun has(feature: FeatureKey): Boolean = entitlements.value.has(feature)

    /** Null means "no ceiling for this field on the current plan." */
    fun limit(selector: (PlanLimits) -> Int?): Int? = selector(entitlements.value.limits)
}
