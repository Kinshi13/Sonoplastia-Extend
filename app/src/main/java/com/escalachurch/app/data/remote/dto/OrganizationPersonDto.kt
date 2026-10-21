package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.domain.model.OrganizationPerson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Read-only mirror of `organization_people` - see [OrganizationPerson]'s doc comment. No
 *  `toDto()` here on purpose: Android doesn't write to this table yet (Fase 11.15-A is
 *  read-only; creating a person is explicitly out of scope until 11.15-B is authorized). */
@Serializable
data class OrganizationPersonDto(
    val id: String? = null,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true
)

fun OrganizationPersonDto.toOrganizationPerson(): OrganizationPerson? {
    val resolvedId = id ?: return null
    return OrganizationPerson(
        id = resolvedId,
        fullName = fullName,
        displayName = displayName,
        isFavorite = isFavorite,
        isActive = isActive
    )
}
