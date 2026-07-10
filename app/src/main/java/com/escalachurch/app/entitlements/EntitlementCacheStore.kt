package com.escalachurch.app.entitlements

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.entitlementDataStore by preferencesDataStore(name = "escala_church_entitlements")

/**
 * Last-known-good snapshot of this church's resolved plan, so the app still knows what to unlock
 * while offline. Per Fase 3 Section 11: this is a bounded-freshness cache, never a source of
 * indefinite premium access - [EntitlementService] only trusts it for [MAX_CACHE_AGE_MS] and
 * always falls back to the hardcoded FREE feature set once the cache goes stale, even if the
 * cached plan was paid.
 */
class EntitlementCacheStore(private val context: Context) {

    private object Keys {
        val PLAN_CODE = stringPreferencesKey("cached_plan_code")
        val STATUS = stringPreferencesKey("cached_subscription_status")
        val FEATURES = stringSetPreferencesKey("cached_features")
        val CACHED_AT = longPreferencesKey("cached_at")
    }

    companion object {
        /** How long a cached entitlement is still allowed to gate premium features while offline. */
        const val MAX_CACHE_AGE_MS = 72 * 60 * 60 * 1000L // 72h
    }

    data class CachedEntitlement(
        val planCode: PlanCode,
        val status: SubscriptionStatus,
        val features: Set<FeatureKey>,
        val cachedAt: Long
    ) {
        fun isFresh(now: Long = System.currentTimeMillis()): Boolean = (now - cachedAt) < MAX_CACHE_AGE_MS
    }

    val cachedFlow: Flow<CachedEntitlement?> = context.entitlementDataStore.data.map { prefs ->
        val cachedAt = prefs[Keys.CACHED_AT] ?: return@map null
        val planCode = prefs[Keys.PLAN_CODE]?.let { runCatching { PlanCode.valueOf(it) }.getOrNull() } ?: return@map null
        val status = prefs[Keys.STATUS]?.let { runCatching { SubscriptionStatus.valueOf(it) }.getOrNull() } ?: SubscriptionStatus.FREE
        val features = (prefs[Keys.FEATURES] ?: emptySet())
            .mapNotNull { runCatching { FeatureKey.valueOf(it) }.getOrNull() }
            .toSet()
        CachedEntitlement(planCode = planCode, status = status, features = features, cachedAt = cachedAt)
    }

    suspend fun current(): CachedEntitlement? = cachedFlow.first()

    suspend fun save(planCode: PlanCode, status: SubscriptionStatus, features: Set<FeatureKey>) {
        context.entitlementDataStore.edit { prefs ->
            prefs[Keys.PLAN_CODE] = planCode.name
            prefs[Keys.STATUS] = status.name
            prefs[Keys.FEATURES] = features.map { it.name }.toSet()
            prefs[Keys.CACHED_AT] = System.currentTimeMillis()
        }
    }
}
