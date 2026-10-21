package com.escalachurch.app.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 11.15 (Etapa C) - the V1 -> canonical V2 conversion must never lose a participant, even
 * though V1's own preview already stopped losing them at the legacy-column level (Fase 11.14).
 * This is one layer deeper: every pipe-separated name becomes its own [CanonicalParticipant].
 */
class ImportV1ToV2AdapterTest {

    private val threeParticipantsText = """
        ESCALA_CHURCH_IMPORT_V1
        [ESCALA]
        data=2026-11-15
        inicio=19:00
        tipo=Culto de domingo
        Louvor=Ana|Pedro|Maria
        Recepção=Carlos|Beatriz
        Sonoplastia=João
    """.trimIndent()

    @Test
    fun `every participant across every role survives the conversion - six in, six out`() {
        val canonical = EscalaChurchImportParser.parse(threeParticipantsText).toCanonicalV2()
        val schedule = canonical.schedules.single()
        val totalParticipants = schedule.roles.sumOf { it.participants.size }
        assertEquals(6, totalParticipants)
    }

    @Test
    fun `participant order within a role is preserved`() {
        val canonical = EscalaChurchImportParser.parse(threeParticipantsText).toCanonicalV2()
        val louvor = canonical.schedules.single().roles.first { it.normalizedRoleName == RoleAliasResolver.REGENCIA }
        assertEquals(listOf("Ana", "Pedro", "Maria"), louvor.participants.map { it.rawName })
    }

    @Test
    fun `role order (position) is preserved as parsed`() {
        val canonical = EscalaChurchImportParser.parse(threeParticipantsText).toCanonicalV2()
        val positions = canonical.schedules.single().roles.sortedBy { it.position }.map { it.normalizedRoleName }
        assertEquals(listOf(RoleAliasResolver.REGENCIA, RoleAliasResolver.RECEPCAO, RoleAliasResolver.SONOPLASTIA), positions)
    }

    @Test
    fun `every participant starts UNRESOLVED - the adapter never reconciles on its own`() {
        val canonical = EscalaChurchImportParser.parse(threeParticipantsText).toCanonicalV2()
        val allParticipants = canonical.schedules.single().roles.flatMap { it.participants }
        assertTrue(allParticipants.all { it.status == ParticipantStatus.UNRESOLVED && it.personId == null })
    }

    @Test
    fun `a resolved date and start time leave no pending fields`() {
        val canonical = EscalaChurchImportParser.parse(threeParticipantsText).toCanonicalV2()
        assertTrue(canonical.schedules.single().pendingFields.isEmpty())
        assertTrue(canonical.schedules.single().isReadyToPublish)
    }

    @Test
    fun `an unparseable date is a pending field, never silently defaulted`() {
        val text = """
            ESCALA_CHURCH_IMPORT_V1
            [ESCALA]
            data=não sei
            tipo=Culto
            Sonoplastia=João
        """.trimIndent()
        val canonical = EscalaChurchImportParser.parse(text).toCanonicalV2()
        val schedule = canonical.schedules.single()
        assertEquals(null, schedule.date)
        assertEquals("não sei", schedule.rawDate)
        assertTrue("date" in schedule.pendingFields)
        assertFalse(schedule.isReadyToPublish)
    }

    @Test
    fun `missing start time is pending, not defaulted to midnight or any guess`() {
        val text = """
            ESCALA_CHURCH_IMPORT_V1
            [ESCALA]
            data=2026-11-15
            tipo=Culto
            Sonoplastia=João
        """.trimIndent()
        val canonical = EscalaChurchImportParser.parse(text).toCanonicalV2()
        assertEquals(null, canonical.schedules.single().startTime)
        assertTrue("startTime" in canonical.schedules.single().pendingFields)
    }

    @Test
    fun `document-level warnings and errors are preserved at the result level`() {
        val text = """
            ESCALA_CHURCH_IMPORT_V1
            [ESCALA]
            data=15/11
            tipo=Culto
            FuncaoQueNaoExiste=Alguém
        """.trimIndent()
        val canonical = EscalaChurchImportParser.parse(text).toCanonicalV2()
        assertTrue(canonical.globalWarnings.any { it.contains("ano", ignoreCase = true) })
        assertTrue(canonical.globalErrors.any { it.contains("não reconhecida") })
    }

    @Test
    fun `contract version is always ESCALA_CHURCH_IMPORT_V2`() {
        val canonical = EscalaChurchImportParser.parse(threeParticipantsText).toCanonicalV2()
        assertEquals(IMPORT_CONTRACT_V2, canonical.contractVersion)
        assertTrue(canonical.isSupportedVersion)
    }
}
