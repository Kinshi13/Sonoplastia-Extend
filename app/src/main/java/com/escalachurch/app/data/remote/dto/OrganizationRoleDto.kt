package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.domain.model.OrganizationRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Read-only mirror of `organization_roles` - see [OrganizationRole]'s doc comment. No `toDto()`
 *  here on purpose: Android doesn't write to this table yet (Fase 11.15-A is read-only). */
@Serializable
data class OrganizationRoleDto(
    val id: String? = null,
    val name: String = "",
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("icon_key") val iconKey: String = "users",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_required") val isRequired: Boolean = false,
    @SerialName("allows_multiple_people") val allowsMultiplePeople: Boolean = false,
    @SerialName("team_id") val teamId: String? = null,
    @SerialName("legacy_field_key") val legacyFieldKey: String? = null
)

fun OrganizationRoleDto.toOrganizationRole(): OrganizationRole? {
    val resolvedId = id ?: return null
    return OrganizationRole(
        id = resolvedId,
        name = name,
        shortName = shortName,
        iconKey = iconKey,
        sortOrder = sortOrder,
        isActive = isActive,
        isRequired = isRequired,
        allowsMultiplePeople = allowsMultiplePeople,
        teamId = teamId,
        legacyFieldKey = legacyFieldKey
    )
}
