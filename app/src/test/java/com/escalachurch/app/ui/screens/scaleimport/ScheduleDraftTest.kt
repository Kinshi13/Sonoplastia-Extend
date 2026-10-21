package com.escalachurch.app.ui.screens.scaleimport

import com.escalachurch.app.domain.importer.RoleAliasResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 11.14 - regression tests for the participant-loss bug found in the Fase 11.13 audit:
 * [ScheduleDraft] used to keep only the first pipe-separated name per role when converting to the
 * five legacy `scales` columns ([ScheduleDraft.firstPersonFor], now removed), silently dropping
 * every other name the admin had already seen and confirmed in the preview. These tests pin down
 * the fix - every name is preserved (joined with "e", the same convention
 * `web/app/admin/actions.ts` already uses for the same columns) - and the single-person case that
 * must keep behaving exactly as before.
 */
class ScheduleDraftTest {

    private fun draft(vararg roles: Pair<String, String>) = ScheduleDraft(
        date = "2026-11-15",
        dateAssumedYear = false,
        startTime = "19:00",
        type = "Culto",
        roles = roles.map { (name, people) -> RoleDraft(roleName = name, peopleText = people) }
    )

    @Test
    fun `multiple pipe-separated names are all preserved, joined with e`() {
        val d = draft(RoleAliasResolver.SONOPLASTIA to "Ana|Pedro|Carlos")
        assertEquals("Ana e Pedro e Carlos", d.soundPerson)
    }

    @Test
    fun `a single name is preserved exactly as before - no regression`() {
        val d = draft(RoleAliasResolver.SONOPLASTIA to "Ana")
        assertEquals("Ana", d.soundPerson)
    }

    @Test
    fun `blank segments between pipes are dropped, not turned into empty names`() {
        val d = draft(RoleAliasResolver.RECEPCAO to "Ana||Pedro| ")
        assertEquals("Ana e Pedro", d.receptionPerson)
    }

    @Test
    fun `a role with no text for this draft resolves to blank, not a crash`() {
        val d = draft(RoleAliasResolver.SONOPLASTIA to "Ana")
        assertEquals("", d.receptionPerson)
    }

    @Test
    fun `hasMultiPersonRole is true when any role has more than one name`() {
        val d = draft(
            RoleAliasResolver.SONOPLASTIA to "Ana|Pedro",
            RoleAliasResolver.RECEPCAO to "Maria"
        )
        assertTrue(d.hasMultiPersonRole())
    }

    @Test
    fun `hasMultiPersonRole is false when every role has at most one name`() {
        val d = draft(
            RoleAliasResolver.SONOPLASTIA to "Ana",
            RoleAliasResolver.RECEPCAO to "Maria"
        )
        assertFalse(d.hasMultiPersonRole())
    }

    @Test
    fun `hasMultiPersonRole is false for an empty role`() {
        val d = draft(RoleAliasResolver.SONOPLASTIA to "")
        assertFalse(d.hasMultiPersonRole())
    }
}
