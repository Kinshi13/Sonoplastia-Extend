package com.escalachurch.app.data.repository

import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.PlanDto
import com.escalachurch.app.data.remote.dto.SubscriptionSummaryDto
import com.escalachurch.app.data.remote.dto.toPlan
import com.escalachurch.app.data.remote.dto.toSubscriptionSummary
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.entitlements.Plan
import com.escalachurch.app.entitlements.SubscriptionSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Plan catalog (public, same for every church) + this church's own subscription row. Read-only
 * from the app on purpose - see 006_plans_entitlements.sql: plans/subscriptions are only ever
 * written via the Stripe webhook or a manual SQL grant, never from a client.
 *
 * Fase 11.9A: `observeSubscription()` follows [ActiveChurchManager.activeChurchId] - the same
 * church every other repository (scales/doxologies/announcements) reads from - instead of
 * resolving its own church_id from the session. Resolving *which* church an authenticated admin
 * belongs to now happens once, in AdminSession.signIn() via ProfileRepository, which then calls
 * ActiveChurchManager.setActiveChurch() - this repository just reacts to that, same as everything
 * else. That also means switching the active church (code entry, "Continuar em", admin login)
 * always refreshes the plan too, for free.
 *
 * The subscription itself goes through `get_church_subscription` (migration 012), a SECURITY
 * DEFINER RPC scoped to one church_id and returning a minimal shape (plan_code/plan_name/
 * features/limits pre-joined, no plan_id/id/source/stripe_*) - never a blanket table read.
 */
class PlanRepository(private val client: SupabaseClient, private val activeChurchManager: ActiveChurchManager) {

    private val plansTable get() = client.postgrest.from(SupabaseTables.PLANS)
    private val refreshTrigger = LocalRefreshTrigger()

    fun observePlans(): Flow<List<Plan>> = client.observeTable(SupabaseTables.PLANS, refreshTrigger) {
        plansTable.select().decodeList<PlanDto>()
            .mapNotNull { it.toPlan() }
            .sortedBy { it.sortOrder }
    }

    /** Forces an immediate re-resolution instead of waiting on the next active-church emission or
     *  foreground event - call after anything that can change what this church is entitled to. */
    fun refresh() = refreshTrigger.bump()

    /** This church's own subscription summary - at most one row, per the `church_id unique`
     *  constraint on `subscriptions`. Never depends on plan_id: the RPC already joins `plans` and
     *  returns plan_code/plan_name/features/limits directly, so there is no separate catalog
     *  lookup to keep in sync here. */
    fun observeSubscription(): Flow<SubscriptionSummary?> = activeChurchManager.activeChurchId.flatMapLatest { churchId ->
        client.observeTable(SupabaseTables.SUBSCRIPTIONS, refreshTrigger) {
            client.postgrest.rpc("get_church_subscription", ChurchIdParam(churchId)).decodeList<SubscriptionSummaryDto>()
        }
    }.map { rows -> rows.mapNotNull { it.toSubscriptionSummary() }.firstOrNull() }

    @Serializable
    private data class ChurchIdParam(@SerialName("p_church_id") val churchId: String)
}
