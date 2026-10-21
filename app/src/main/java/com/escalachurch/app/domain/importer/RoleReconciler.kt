package com.escalachurch.app.domain.importer

import com.escalachurch.app.domain.model.OrganizationRole
import java.text.Normalizer

/**
 * Fase 11.15 (Etapa D/F) - resolves each [CanonicalRole.roleId] against a church's real
 * `organization_roles`, fetched once by the caller (see
 * [com.escalachurch.app.data.repository.OrganizationRoleRepository]). A role the admin renamed or
 * created from scratch on the web (no `legacy_field_key`) still resolves here, by name - only a
 * role with no matching name AND no matching legacy alias stays unresolved (`roleId = null`,
 * added to the schedule's [CanonicalSchedule.pendingFields]) rather than being invented on the
 * spot (Etapa D: "não criar função nova automaticamente sem confirmação").
 */
object RoleReconciler {

    /** `organization_roles.legacy_field_key` -> the same canonical name [RoleAliasResolver]
     *  already uses for the five original functions - lets a raw "Som" resolve to a real,
     *  possibly admin-renamed role row via the legacy link, not just by literal name match. */
    private val legacyFieldToCanonical = mapOf(
        "reception_person" to RoleAliasResolver.RECEPCAO,
        "sound_person" to RoleAliasResolver.SONOPLASTIA,
        "preaching_person" to RoleAliasResolver.PREGACAO,
        "conducting_person" to RoleAliasResolver.REGENCIA,
        "musical_message_person" to RoleAliasResolver.MENSAGEM_MUSICAL
    )

    fun reconcile(result: CanonicalImportResult, knownRoles: List<OrganizationRole>): CanonicalImportResult {
        if (knownRoles.isEmpty()) return result

        val byName = knownRoles.associateBy { normalize(it.name) }
        val byLegacyCanonical = knownRoles.mapNotNull { role ->
            val canonical = legacyFieldToCanonical[role.legacyFieldKey] ?: return@mapNotNull null
            normalize(canonical) to role
        }.toMap()

        return result.copy(schedules = result.schedules.map { schedule -> reconcileSchedule(schedule, byName, byLegacyCanonical) })
    }

    private fun reconcileSchedule(
        schedule: CanonicalSchedule,
        byName: Map<String, OrganizationRole>,
        byLegacyCanonical: Map<String, OrganizationRole>
    ): CanonicalSchedule {
        val roles = schedule.roles.map { role ->
            if (role.roleId != null) return@map role // already resolved - never override a decision
            val match = byName[normalize(role.normalizedRoleName)]
                ?: byName[normalize(role.rawRoleName)]
                ?: byLegacyCanonical[normalize(role.normalizedRoleName)]
            if (match != null) role.copy(roleId = match.id) else role
        }
        val stillUnresolved = roles.filter { it.roleId == null }.map { "role:${it.rawRoleName}" }
        return schedule.copy(roles = roles, pendingFields = (schedule.pendingFields + stillUnresolved).distinct())
    }

    private fun normalize(text: String): String =
        text.trim().lowercase().let { Normalizer.normalize(it, Normalizer.Form.NFD) }.replace(Regex("\\p{Mn}+"), "")
}
