package com.escalachurch.app.domain.importer

import java.time.format.DateTimeFormatter

/**
 * Fase 11.15 (Etapa C) - converts a V1 [ImportResult] (see EscalaChurchImportParser) into the
 * canonical V2 model, with zero data loss: every [ImportedPerson] in every [ImportedRole] becomes
 * its own [CanonicalParticipant] - this is the exact fix the Fase 11.15 brief asked for, one
 * layer up from Fase 11.14's text-level fix (which only stopped the legacy *columns* from
 * dropping names; this is what lets Android eventually write real `scale_assignments` rows
 * without ever having collapsed "Ana, Pedro e Maria" into one string in the first place).
 *
 * This adapter does NOT reconcile participants/roles against the church's real people/role bank
 * - it has no database access by design, same as the V1 parser itself. See
 * [com.escalachurch.app.domain.importer] `ParticipantReconciler`/`RoleReconciler` for that next
 * step, which the caller runs separately once it has fetched the church's own lists.
 */
fun ImportResult.toCanonicalV2(): CanonicalImportResult {
    val canonicalSchedules = schedules.map { it.toCanonicalSchedule() }
    return CanonicalImportResult(
        contractVersion = IMPORT_CONTRACT_V2,
        schedules = canonicalSchedules,
        // V1's parser (EscalaChurchImportParser) reports warnings/errors for the whole document,
        // not scoped to one [ESCALA] block - there's no block boundary information left by the
        // time ImportResult exists to attribute them more precisely without guessing, so they
        // stay at this level rather than being falsely assigned to one schedule.
        globalWarnings = warnings.map { it.message },
        globalErrors = errors.map { it.message }
    )
}

private fun ImportedSchedule.toCanonicalSchedule(): CanonicalSchedule {
    val roles = this.roles.mapIndexed { index, role -> role.toCanonicalRole(position = index) }
    val isoDate = date?.format(DateTimeFormatter.ISO_LOCAL_DATE)
    return CanonicalSchedule(
        title = type,
        rawDate = rawDate,
        date = isoDate,
        dateAssumedYear = dateAssumedYear,
        startTime = startTime,
        type = type,
        source = ImportSource.MANUAL_TEXT_V1,
        roles = roles,
        pendingFields = buildList {
            if (isoDate == null) add("date")
            if (startTime.isNullOrBlank()) add("startTime")
        }
    )
}

private fun ImportedRole.toCanonicalRole(position: Int): CanonicalRole = CanonicalRole(
    roleId = null, // resolved later by RoleReconciler, once a church's real roles are known
    rawRoleName = rawRoleText,
    normalizedRoleName = roleName,
    position = position,
    // Fase 11.15 - this is the line that didn't exist for the legacy-column path: every person
    // becomes its own entry, in the order they were typed, none dropped.
    participants = people.map { CanonicalParticipant(rawName = it.rawName) }
)
