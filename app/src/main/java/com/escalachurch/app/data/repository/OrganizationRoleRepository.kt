package com.escalachurch.app.data.repository

import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.OrganizationRoleDto
import com.escalachurch.app.data.remote.dto.toOrganizationRole
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.domain.model.OrganizationRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Fase 11.15 (Importação Inteligente V2, Etapa A/D) - read-only access to the church's
 * `organization_roles` (already created and populated by migration 010, confirmed APPLIED in
 * production). Used only to let the import preview resolve a parsed role name ("Louvor") against
 * a real role the web admin may already have renamed/customized - never to write. RLS on this
 * table is `public read` (unchanged from the raw migration), so this works for any signed-in
 * session, admin or not.
 */
class OrganizationRoleRepository(private val client: SupabaseClient, private val activeChurchManager: ActiveChurchManager) {

    private val table get() = client.postgrest.from(SupabaseTables.ORGANIZATION_ROLES)
    private val refreshTrigger = LocalRefreshTrigger()

    fun observeActive(): Flow<List<OrganizationRole>> = activeChurchManager.activeChurchId.flatMapLatest { churchId ->
        client.observeTable(SupabaseTables.ORGANIZATION_ROLES, refreshTrigger) {
            table.select { filter { eq("church_id", churchId); eq("is_active", true) } }
                .decodeList<OrganizationRoleDto>()
                .mapNotNull { it.toOrganizationRole() }
                .sortedBy { it.sortOrder }
        }
    }
}
