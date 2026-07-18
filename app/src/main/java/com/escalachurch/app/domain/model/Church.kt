package com.escalachurch.app.domain.model

/** Result of looking a church up by its public code (== `churches.slug` today - see migration
 *  013_public_church_lookup.sql; there is no separate code column). */
data class Church(
    val id: String,
    val slug: String,
    val name: String,
    val isActive: Boolean
)

/**
 * One church the user has previously entered, for the "Continuar em" list on the entry screen.
 * Deliberately minimal - no password, no admin token, nothing that would need to be treated as a
 * credential. Losing/leaking this list only reveals which churches this device visited, never how
 * to administer them.
 */
data class RecentChurch(
    val churchId: String,
    val churchCode: String,
    val churchName: String,
    val logoUrl: String? = null,
    val lastAccessedAt: Long,
    val isFavorite: Boolean = false
)
