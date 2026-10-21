package com.escalachurch.app.domain.importer

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Fase 11.15-B - pure functions that turn one ready-to-publish [CanonicalSchedule] into the two
 * JSON payloads `import_schedule_with_assignments` (see
 * supabase/migrations/018_import_transactional_rpc.sql - PROPOSED, not yet applied to
 * production) expects. No network, no Supabase client here - this is the part that's directly
 * testable without a real backend; see
 * [com.escalachurch.app.data.repository.ScaleAssignmentImportRepository] for the actual RPC call.
 *
 * Deliberately refuses to build a payload for a schedule that isn't [CanonicalSchedule.isReadyToPublish]
 * - there is no "best effort" here the way the legacy-column projection below is best-effort;
 * calling this on an unready schedule is a programmer error, not a recoverable one.
 */
object ImportRpcPayloadBuilder {

    /** `p_schedule` - matches `scales`' own columns. Fase 11.15-B also derives the five legacy
     *  columns best-effort (multiple participants in the same legacy-mapped role joined with
     *  "e", same convention `web/app/admin/actions.ts` already uses) so the row stays readable
     *  by every existing reader that still only looks at those columns - the same precedent
     *  behind migration 010/011. */
    fun buildSchedulePayload(schedule: CanonicalSchedule): JsonObject {
        require(schedule.isReadyToPublish) { "schedule ${schedule.localId} is not ready to publish" }
        return buildJsonObject {
            put("date", schedule.date?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)
            put("start_time", schedule.startTime?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)
            put("end_time", schedule.endTime?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)
            put("type", JsonPrimitive(schedule.type))
            put("title", JsonPrimitive(schedule.title.ifBlank { schedule.type }))
            put("reception_person", JsonPrimitive(legacyPersonFor(schedule, RoleAliasResolver.RECEPCAO)))
            put("sound_person", JsonPrimitive(legacyPersonFor(schedule, RoleAliasResolver.SONOPLASTIA)))
            put("preaching_person", JsonPrimitive(legacyPersonFor(schedule, RoleAliasResolver.PREGACAO)))
            put("conducting_person", JsonPrimitive(legacyPersonFor(schedule, RoleAliasResolver.REGENCIA)))
            put("musical_message_person", JsonPrimitive(legacyPersonFor(schedule, RoleAliasResolver.MENSAGEM_MUSICAL)))
            put("notes", JsonPrimitive(schedule.notes))
            put("is_special_event", JsonPrimitive(schedule.isSpecialEvent))
            put("is_temporary", JsonPrimitive(schedule.isTemporary))
            put("source_type", JsonPrimitive("OFFICIAL"))
        }
    }

    /** `p_assignments` - one object per [CanonicalParticipant] across every role, in the exact
     *  order they'll be saved ([CanonicalRole.position] first, then participant order within the
     *  role) - this `position` becomes `scale_assignments.position`. Every `role_id` here is a
     *  real `organization_roles.id` - [CanonicalSchedule.isReadyToPublish] (checked above) is
     *  exactly what guarantees that, since an unresolved role keeps the schedule in
     *  `pendingFields`. */
    fun buildAssignmentsPayload(schedule: CanonicalSchedule): JsonArray {
        require(schedule.isReadyToPublish) { "schedule ${schedule.localId} is not ready to publish" }
        var position = 0
        return buildJsonArray {
            schedule.roles.sortedBy { it.position }.forEach { role ->
                role.participants.forEach { participant ->
                    add(buildJsonObject {
                        put("role_id", JsonPrimitive(requireNotNull(role.roleId) { "role ${role.rawRoleName} has no roleId" }))
                        put("person_id", participant.personId?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)
                        put(
                            "custom_person_name",
                            if (participant.personId == null) JsonPrimitive(participant.resolvedName ?: participant.rawName)
                            else kotlinx.serialization.json.JsonNull
                        )
                        put("role_name_snapshot", JsonPrimitive(role.normalizedRoleName))
                        put("person_name_snapshot", JsonPrimitive(participant.resolvedName ?: participant.rawName))
                        put("position", JsonPrimitive(position))
                        put("notes", JsonPrimitive(participant.notes ?: ""))
                    })
                    position++
                }
            }
        }
    }

    /** Best-effort projection onto one of the five legacy `scales` columns - every participant of
     *  every role whose [CanonicalRole.normalizedRoleName] matches [canonicalRoleName], joined
     *  with "e" (same convention `web/app/admin/actions.ts` already uses for the same columns).
     *  Never the source of truth once [buildAssignmentsPayload] exists - this is read-compatibility
     *  for whatever still only looks at `scales` directly (older Android builds, exports). */
    private fun legacyPersonFor(schedule: CanonicalSchedule, canonicalRoleName: String): String =
        schedule.roles
            .filter { it.normalizedRoleName == canonicalRoleName }
            .flatMap { it.participants }
            .mapNotNull { (it.resolvedName ?: it.rawName).takeIf { name -> name.isNotBlank() } }
            .joinToString(" e ")
}
