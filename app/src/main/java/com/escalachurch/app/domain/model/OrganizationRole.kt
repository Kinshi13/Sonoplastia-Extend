package com.escalachurch.app.domain.model

/**
 * Mirrors `public.organization_roles` (supabase/migrations/010_organization_roles_people.sql,
 * confirmed APPLIED in production - see supabase/MIGRATION_INVENTORY.md). The web admin has
 * written to this table since Fase 11.8.4; Android only reads it (Fase 11.15, for Importação
 * Inteligente V2's role reconciliation) - no write path exists yet.
 */
data class OrganizationRole(
    val id: String = "",
    val name: String = "",
    val shortName: String? = null,
    val iconKey: String = "users",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val isRequired: Boolean = false,
    val allowsMultiplePeople: Boolean = false,
    val teamId: String? = null,
    /** One of the five legacy `scales` columns this role mirrors, or null for a role the admin
     *  created from scratch on the web (a "função personalizada" - see RoleReconciler). */
    val legacyFieldKey: String? = null
)
