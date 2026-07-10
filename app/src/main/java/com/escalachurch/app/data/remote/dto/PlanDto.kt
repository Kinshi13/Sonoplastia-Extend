package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.entitlements.FeatureKey
import com.escalachurch.app.entitlements.Plan
import com.escalachurch.app.entitlements.PlanCode
import com.escalachurch.app.entitlements.PlanLimits
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlanLimitsDto(
    val maxAdmins: Int? = null,
    val maxPersonalEvents: Int? = null,
    val maxPersonalCards: Int? = null,
    val historyMonths: Int? = null,
    val maxAnnouncements: Int? = null,
    val maxMediaStorageMb: Int? = null,
    val maxOrganizations: Int? = null
)

@Serializable
data class PlanDto(
    val id: String? = null,
    val code: String = "FREE",
    val name: String = "",
    val description: String = "",
    @SerialName("monthly_price_cents") val monthlyPriceCents: Int? = null,
    @SerialName("yearly_price_cents") val yearlyPriceCents: Int? = null,
    val currency: String = "BRL",
    @SerialName("billing_period") val billingPeriod: String = "recurring",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val features: List<String> = emptyList(),
    val limits: PlanLimitsDto = PlanLimitsDto()
)

fun PlanDto.toPlan(): Plan? {
    return Plan(
        id = id ?: return null,
        code = PlanCode.fromCode(code),
        name = name,
        description = description,
        monthlyPriceCents = monthlyPriceCents,
        yearlyPriceCents = yearlyPriceCents,
        currency = currency,
        billingPeriod = billingPeriod,
        isActive = isActive,
        isPublic = isPublic,
        sortOrder = sortOrder,
        features = features.mapNotNull { name -> runCatching { FeatureKey.valueOf(name) }.getOrNull() }.toSet(),
        limits = PlanLimits(
            maxAdmins = limits.maxAdmins,
            maxPersonalEvents = limits.maxPersonalEvents,
            maxPersonalCards = limits.maxPersonalCards,
            historyMonths = limits.historyMonths,
            maxAnnouncements = limits.maxAnnouncements,
            maxMediaStorageMb = limits.maxMediaStorageMb,
            maxOrganizations = limits.maxOrganizations
        )
    )
}
