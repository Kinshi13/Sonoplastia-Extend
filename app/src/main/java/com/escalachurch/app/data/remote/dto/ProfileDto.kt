package com.escalachurch.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors just the columns PlanRepository needs from `profiles` to resolve which church an
 * authenticated (admin) session actually belongs to - never trusted from BuildConfig once there's
 * a real session, see PlanRepository.observeChurchId().
 */
@Serializable
data class ProfileDto(
    val id: String? = null,
    @SerialName("church_id") val churchId: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false
)
