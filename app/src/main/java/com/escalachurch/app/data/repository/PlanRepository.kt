package com.escalachurch.app.data.repository

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.PlanDto
import com.escalachurch.app.data.remote.dto.SubscriptionDto
import com.escalachurch.app.data.remote.dto.toPlan
import com.escalachurch.app.data.remote.dto.toSubscription
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.entitlements.Plan
import com.escalachurch.app.entitlements.Subscription
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Plan catalog (public, same for every church) + this church's own subscription row. Read-only
 * from the app on purpose - see 006_plans_entitlements.sql: plans/subscriptions are only ever
 * written via the Stripe webhook or a manual SQL grant, never from a client.
 */
class PlanRepository(private val client: SupabaseClient) {

    private val plansTable get() = client.postgrest.from(SupabaseTables.PLANS)
    private val subscriptionsTable get() = client.postgrest.from(SupabaseTables.SUBSCRIPTIONS)
    private val refreshTrigger = LocalRefreshTrigger()

    fun observePlans(): Flow<List<Plan>> = client.observeTable(SupabaseTables.PLANS, refreshTrigger) {
        plansTable.select().decodeList<PlanDto>()
            .mapNotNull { it.toPlan() }
            .sortedBy { it.sortOrder }
    }

    /** This church's own subscription - at most one row, per the `church_id unique` constraint. */
    fun observeSubscription(): Flow<Subscription?> = client.observeTable(SupabaseTables.SUBSCRIPTIONS, refreshTrigger) {
        subscriptionsTable.select { filter { eq("church_id", BuildConfig.CHURCH_ID) } }
            .decodeList<SubscriptionDto>()
            .mapNotNull { it.toSubscription() }
    }.map { it.firstOrNull() }
}
