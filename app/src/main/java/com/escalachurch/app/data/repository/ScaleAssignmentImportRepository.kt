package com.escalachurch.app.data.repository

import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.domain.importer.CanonicalSchedule
import com.escalachurch.app.domain.importer.ImportRpcPayloadBuilder
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Fase 11.15-B - calls `import_schedule_with_assignments` (see
 * supabase/migrations/018_import_transactional_rpc.sql), a single atomic, idempotent RPC that
 * creates one `scales` row and every one of its `scale_assignments` together, or none of them.
 *
 * IMPORTANT - this RPC does NOT exist in production yet (state: CREATED, see
 * supabase/MIGRATION_INVENTORY.md). Calling [importSchedule] against a project where migration
 * 018 hasn't been applied will fail with a PostgREST "function not found" error. Nothing in the
 * app's current import screen calls this class yet - see ScaleImportViewModel, which still only
 * writes the five legacy `scales` columns directly - wiring it up is a separate, deliberate step
 * that should happen only after 018 is actually applied and verified (VALIDATE_018_...sql).
 *
 * [operationId] must be the same value across a retry of the same schedule draft (see
 * [CanonicalSchedule.localId], already a stable per-draft UUID) - that's what makes a reenviado
 * após timeout/duplo-toque idempotent at the database level, not just in the UI.
 */
class ScaleAssignmentImportRepository(private val client: SupabaseClient, private val activeChurchManager: ActiveChurchManager) {

    suspend fun importSchedule(schedule: CanonicalSchedule): String {
        require(schedule.isReadyToPublish) { "schedule ${schedule.localId} is not ready to publish" }
        val churchId = activeChurchManager.activeChurchId.first()
        val params = ImportRpcParams(
            operationId = schedule.localId,
            churchId = churchId,
            schedule = ImportRpcPayloadBuilder.buildSchedulePayload(schedule),
            assignments = ImportRpcPayloadBuilder.buildAssignmentsPayload(schedule)
        )
        return client.postgrest.rpc("import_schedule_with_assignments", params).decodeAs<String>()
    }

    @Serializable
    private data class ImportRpcParams(
        @SerialName("p_operation_id") val operationId: String,
        @SerialName("p_church_id") val churchId: String,
        @SerialName("p_schedule") val schedule: JsonObject,
        @SerialName("p_assignments") val assignments: JsonArray
    )
}
