package com.escalachurch.app.data.repository

import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.OrganizationPersonDto
import com.escalachurch.app.data.remote.dto.toOrganizationPerson
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.domain.model.OrganizationPerson
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Fase 11.15 (Importação Inteligente V2, Etapa A/F) - read-only access to the church's
 * `organization_people` (already created and populated by migration 010, confirmed APPLIED in
 * production). Used only to let the import preview suggest/confirm a match for a parsed name
 * ("João") against the church's real people bank - never to write, never to create a person
 * automatically (see [com.escalachurch.app.domain.importer.ParticipantReconciler]).
 *
 * Unlike `organization_roles`, this table's real RLS is stricter than the raw migration file:
 * `admin read own church` (confirmed in supabase/MIGRATION_INVENTORY.md), not `public read`. A
 * non-admin session gets an empty list here, not an error - reconciliation then simply has
 * nothing to match against, which is already handled (every name just stays unresolved).
 */
class OrganizationPersonRepository(private val client: SupabaseClient, private val activeChurchManager: ActiveChurchManager) {

    private val table get() = client.postgrest.from(SupabaseTables.ORGANIZATION_PEOPLE)
    private val refreshTrigger = LocalRefreshTrigger()

    fun observeActive(): Flow<List<OrganizationPerson>> = activeChurchManager.activeChurchId.flatMapLatest { churchId ->
        client.observeTable(SupabaseTables.ORGANIZATION_PEOPLE, refreshTrigger) {
            table.select { filter { eq("church_id", churchId); eq("is_active", true) } }
                .decodeList<OrganizationPersonDto>()
                .mapNotNull { it.toOrganizationPerson() }
                .sortedBy { it.fullName }
        }
    }
}
