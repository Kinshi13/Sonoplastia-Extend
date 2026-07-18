package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.domain.model.Church
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the `get_church_by_code` RPC's return row (migration 013) - not the `churches` table
 *  directly, so an extra column added to `churches` later never leaks here unnoticed. */
@Serializable
data class ChurchDto(
    val id: String? = null,
    val slug: String? = null,
    val name: String? = null,
    @SerialName("is_active") val isActive: Boolean = false
)

fun ChurchDto.toChurch(): Church? {
    return Church(
        id = id ?: return null,
        slug = slug ?: return null,
        name = name ?: "",
        isActive = isActive
    )
}
