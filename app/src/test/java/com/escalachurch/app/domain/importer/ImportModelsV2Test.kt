package com.escalachurch.app.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportModelsV2Test {

    private val sample = CanonicalImportResult(
        schedules = listOf(
            CanonicalSchedule(
                title = "Culto de domingo",
                date = "2026-11-15",
                startTime = "19:00",
                roles = listOf(
                    CanonicalRole(
                        rawRoleName = "Louvor",
                        normalizedRoleName = RoleAliasResolver.REGENCIA,
                        participants = listOf(
                            CanonicalParticipant(rawName = "Ana"),
                            CanonicalParticipant(rawName = "Pedro", status = ParticipantStatus.SUGGESTED, candidateNames = listOf("Pedro Souza"))
                        )
                    )
                )
            )
        )
    )

    @Test
    fun `round trips through JSON without losing any participant`() {
        val json = sample.toJson()
        val parsed = parseCanonicalImportJson(json)
        assertEquals(2, parsed.schedules.single().roles.single().participants.size)
        assertEquals(sample, parsed)
    }

    @Test
    fun `an empty payload is a global error, not a crash`() {
        val result = parseCanonicalImportJson("")
        assertTrue(result.schedules.isEmpty())
        assertTrue(result.globalErrors.isNotEmpty())
    }

    @Test
    fun `garbage JSON is a global error, not a crash`() {
        val result = parseCanonicalImportJson("{not valid json at all")
        assertTrue(result.schedules.isEmpty())
        assertTrue(result.globalErrors.isNotEmpty())
    }

    @Test
    fun `an unknown contract version is rejected explicitly`() {
        val json = """{"contractVersion":"ESCALA_CHURCH_IMPORT_V99","schedules":[]}"""
        val result = parseCanonicalImportJson(json)
        assertFalse(result.isSupportedVersion)
        assertTrue(result.globalErrors.any { it.contains("ESCALA_CHURCH_IMPORT_V99") })
    }

    @Test
    fun `unknown extra fields are ignored, not treated as errors - future compatibility`() {
        val json = """
            {"contractVersion":"$IMPORT_CONTRACT_V2","schedules":[],"somethingFromTheFuture":42}
        """.trimIndent()
        val result = parseCanonicalImportJson(json)
        assertTrue(result.isSupportedVersion)
        assertTrue(result.globalErrors.isEmpty())
    }

    @Test
    fun `a schedule with no pending fields and no errors is ready to publish`() {
        assertTrue(sample.schedules.single().isReadyToPublish)
    }

    @Test
    fun `a schedule with a pending field is not ready to publish`() {
        val pending = sample.schedules.single().copy(pendingFields = listOf("date"))
        assertFalse(pending.isReadyToPublish)
    }
}
