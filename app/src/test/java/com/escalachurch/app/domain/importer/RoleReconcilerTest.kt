package com.escalachurch.app.domain.importer

import com.escalachurch.app.domain.model.OrganizationRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleReconcilerTest {

    private val seededSonoplastia = OrganizationRole(id = "r-sound", name = "Sonoplastia", legacyFieldKey = "sound_person")
    private val customRole = OrganizationRole(id = "r-custom", name = "Mídia e Transmissão", legacyFieldKey = null)

    private fun resultWith(role: CanonicalRole): CanonicalImportResult =
        CanonicalImportResult(schedules = listOf(CanonicalSchedule(roles = listOf(role))))

    @Test
    fun `a standard role resolves via its seeded legacy_field_key`() {
        val role = CanonicalRole(rawRoleName = "Som", normalizedRoleName = RoleAliasResolver.SONOPLASTIA)
        val reconciled = RoleReconciler.reconcile(resultWith(role), listOf(seededSonoplastia))
        assertEquals("r-sound", reconciled.schedules.single().roles.single().roleId)
    }

    @Test
    fun `a custom web-only role resolves by exact name match, with no legacy alias involved`() {
        // Not something RoleAliasResolver would ever resolve on its own - this role only exists
        // because an admin created it on the web (organization_roles, no legacy_field_key).
        val role = CanonicalRole(rawRoleName = "Mídia e Transmissão", normalizedRoleName = "Mídia e Transmissão")
        val reconciled = RoleReconciler.reconcile(resultWith(role), listOf(customRole))
        assertEquals("r-custom", reconciled.schedules.single().roles.single().roleId)
    }

    @Test
    fun `an unresolved role stays null and is marked pending - never invented`() {
        val role = CanonicalRole(rawRoleName = "Decoração", normalizedRoleName = "Decoração")
        val reconciled = RoleReconciler.reconcile(resultWith(role), listOf(seededSonoplastia))
        val reconciledRole = reconciled.schedules.single().roles.single()
        assertNull(reconciledRole.roleId)
        assertTrue("role:Decoração" in reconciled.schedules.single().pendingFields)
    }

    @Test
    fun `an empty role bank leaves the result untouched`() {
        val role = CanonicalRole(rawRoleName = "Som", normalizedRoleName = RoleAliasResolver.SONOPLASTIA)
        val original = resultWith(role)
        assertEquals(original, RoleReconciler.reconcile(original, emptyList()))
    }

    @Test
    fun `a role already resolved is never overridden`() {
        val role = CanonicalRole(roleId = "already-set", rawRoleName = "Som", normalizedRoleName = RoleAliasResolver.SONOPLASTIA)
        val reconciled = RoleReconciler.reconcile(resultWith(role), listOf(seededSonoplastia))
        assertEquals("already-set", reconciled.schedules.single().roles.single().roleId)
    }
}
