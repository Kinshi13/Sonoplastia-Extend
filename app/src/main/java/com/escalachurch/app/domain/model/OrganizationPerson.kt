package com.escalachurch.app.domain.model

/**
 * Mirrors `public.organization_people` (supabase/migrations/010_organization_roles_people.sql,
 * confirmed APPLIED in production - see supabase/MIGRATION_INVENTORY.md). RLS on this table is
 * stricter than the raw migration file: `admin read own church` (not public), so
 * [com.escalachurch.app.data.repository.OrganizationPersonRepository] only ever gets useful data
 * under an authenticated admin session - see that repository's own doc comment.
 */
data class OrganizationPerson(
    val id: String = "",
    val fullName: String = "",
    val displayName: String? = null,
    val isFavorite: Boolean = false,
    val isActive: Boolean = true
) {
    /** What a human reads/types for this person - [displayName] when set, [fullName] otherwise.
     *  Used as the matching target in [com.escalachurch.app.domain.importer.NameMatcher]. */
    val nameForMatching: String get() = displayName?.takeIf { it.isNotBlank() } ?: fullName
}
