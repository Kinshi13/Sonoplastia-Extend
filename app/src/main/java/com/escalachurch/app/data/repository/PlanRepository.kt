package com.escalachurch.app.data.repository

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.PlanDto
import com.escalachurch.app.data.remote.dto.ProfileDto
import com.escalachurch.app.data.remote.dto.SubscriptionSummaryDto
import com.escalachurch.app.data.remote.dto.toPlan
import com.escalachurch.app.data.remote.dto.toSubscriptionSummary
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.entitlements.Plan
import com.escalachurch.app.entitlements.SubscriptionSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Plan catalog (public, same for every church) + this church's own subscription row. Read-only
 * from the app on purpose - see 006_plans_entitlements.sql: plans/subscriptions are only ever
 * written via the Stripe webhook or a manual SQL grant, never from a client.
 *
 * Hotfix (Fase 11.9, escopo reduzido): `subscriptions` used to be read straight off the table,
 * filtered by the church baked into this build at compile time (BuildConfig.CHURCH_ID). Once
 * `subscriptions`' public RLS read was locked down (SAFE migration), that direct read started
 * failing with 42501 for every session that isn't authenticated - which is nearly all of them,
 * since only Admin has a login screen; ordinary "public view" usage never signs in. That's why
 * PRO never loaded, for any church, not just one.
 *
 * Fixed three ways, all applied here:
 *  1. `observeSubscription()` now goes through `get_church_subscription` (migration 012), a
 *     SECURITY DEFINER RPC scoped to one church_id, instead of a blanket table read - restores
 *     access without reopening the table the SAFE migration deliberately closed.
 *  2. The church_id used is resolved from the *signed-in admin's own* `profiles.church_id` when
 *     there's a session, never BuildConfig.CHURCH_ID once authenticated - so an admin account can
 *     never inherit a different church's plan just because this particular build was compiled
 *     pinned to that church (see observeChurchId()).
 *  3. The RPC's own return shape is minimal by design (see migration 012's header): no
 *     plan_id/id/source/stripe_* columns, ever - it returns plan_code/plan_name/features/limits
 *     pre-joined, so a row can never carry a billing identifier this client has no business
 *     seeing, even if `subscriptions` grows one later.
 */
class PlanRepository(private val client: SupabaseClient) {

    private val plansTable get() = client.postgrest.from(SupabaseTables.PLANS)
    private val refreshTrigger = LocalRefreshTrigger()

    fun observePlans(): Flow<List<Plan>> = client.observeTable(SupabaseTables.PLANS, refreshTrigger) {
        plansTable.select().decodeList<PlanDto>()
            .mapNotNull { it.toPlan() }
            .sortedBy { it.sortOrder }
    }

    /** Forces an immediate re-resolution instead of waiting on the next SessionStatus emission or
     *  foreground event - call after anything that can change what this church is entitled to. */
    fun refresh() = refreshTrigger.bump()

    /** The church whose subscription should be resolved right now. Null means "not known yet"
     *  (session still loading from storage) - callers must not fetch on a null churchId, since
     *  that would momentarily resolve against the wrong (BuildConfig) church while a real admin
     *  session is still being restored. Public so EntitlementService can track "which church is
     *  this session for" independently of whether the subscription fetch itself succeeds. */
    fun observeChurchId(): Flow<String?> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.id?.let { fetchProfileChurchId(it) } ?: BuildConfig.CHURCH_ID
            is SessionStatus.NotAuthenticated, is SessionStatus.NetworkError -> BuildConfig.CHURCH_ID
            is SessionStatus.LoadingFromStorage -> null
        }
    }.distinctUntilChanged()

    private suspend fun fetchProfileChurchId(userId: String): String? = runCatching {
        client.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<ProfileDto>()
            ?.churchId
    }.getOrNull()

    /** This church's own subscription summary - at most one row, per the `church_id unique`
     *  constraint on `subscriptions`. Never depends on plan_id: the RPC already joins `plans` and
     *  returns plan_code/plan_name/features/limits directly, so there is no separate catalog
     *  lookup to keep in sync here. */
    fun observeSubscription(): Flow<SubscriptionSummary?> = observeChurchId().filterNotNull().flatMapLatest { churchId ->
        client.observeTable(SupabaseTables.SUBSCRIPTIONS, refreshTrigger) {
            client.postgrest.rpc("get_church_subscription", ChurchIdParam(churchId)).decodeList<SubscriptionSummaryDto>()
        }
    }.map { rows -> rows.mapNotNull { it.toSubscriptionSummary() }.firstOrNull() }

    @Serializable
    private data class ChurchIdParam(@SerialName("p_church_id") val churchId: String)
}
