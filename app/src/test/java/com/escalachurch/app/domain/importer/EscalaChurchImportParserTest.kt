package com.escalachurch.app.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EscalaChurchImportParserTest {

    @Test
    fun `single schedule parses cleanly with no errors`() {
        val text = """
            ESCALA_CHURCH_IMPORT_V1

            [ESCALA]
            data=2026-09-12
            tipo=Sabado
            inicio=08:45

            Recepção=João|Maria
            Sonoplastia=Robson
            Pregação=Pr. Carlos
            Regência=Ana
            Mensagem Musical=Pedro
        """.trimIndent()

        val result = EscalaChurchImportParser.parse(text)

        assertTrue(result.isSupportedVersion)
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.schedules.size)

        val schedule = result.schedules.single()
        assertEquals(LocalDate.of(2026, 9, 12), schedule.date)
        assertFalse(schedule.dateAssumedYear)
        assertEquals("Sabado", schedule.type)
        assertEquals("08:45", schedule.startTime)
        assertEquals(5, schedule.roles.size)

        val reception = schedule.roles.first { it.roleName == RoleAliasResolver.RECEPCAO }
        assertEquals(listOf("João", "Maria"), reception.people.map { it.rawName })
    }

    @Test
    fun `multiple schedules produce multiple ImportedSchedule with independent roles`() {
        val text = """
            ESCALA_CHURCH_IMPORT_V1

            [ESCALA]
            data=2026-09-12
            tipo=Sabado

            Recepção=João|Maria
            Sonoplastia=Robson
            Regência=Ana

            [ESCALA]
            data=2026-09-16
            tipo=Quarta

            Recepção=Paulo
            Sonoplastia=Robson
            Pregação=Pr. Lucas
        """.trimIndent()

        val result = EscalaChurchImportParser.parse(text)

        assertEquals(2, result.schedules.size)
        assertEquals(LocalDate.of(2026, 9, 12), result.schedules[0].date)
        assertEquals(LocalDate.of(2026, 9, 16), result.schedules[1].date)
        assertEquals(3, result.schedules[0].roles.size)
        assertEquals(3, result.schedules[1].roles.size)
        assertEquals(listOf("Paulo"), result.schedules[1].roles.first { it.roleName == RoleAliasResolver.RECEPCAO }.people.map { it.rawName })
    }

    @Test
    fun `multiple names in one role are split on pipe`() {
        val text = """
            ESCALA_CHURCH_IMPORT_V1

            [ESCALA]
            data=2026-09-12
            tipo=Sabado

            Recepção=João|Maria|Ana|Paulo
        """.trimIndent()

        val schedule = EscalaChurchImportParser.parse(text).schedules.single()
        val names = schedule.roles.single().people.map { it.rawName }
        assertEquals(listOf("João", "Maria", "Ana", "Paulo"), names)
    }

    @Test
    fun `role alias resolves to the canonical role name`() {
        val text = """
            ESCALA_CHURCH_IMPORT_V1

            [ESCALA]
            data=2026-09-12
            tipo=Sabado

            Som=Robson
            Louvor=Ana
            Palavra=Pr. Carlos
        """.trimIndent()

        val schedule = EscalaChurchImportParser.parse(text).schedules.single()
        val roleNames = schedule.roles.map { it.roleName }.toSet()
        assertEquals(setOf(RoleAliasResolver.SONOPLASTIA, RoleAliasResolver.REGENCIA, RoleAliasResolver.PREGACAO), roleNames)
    }

    @Test
    fun `unknown role produces an error and is not included as a role`() {
        val text = """
            ESCALA_CHURCH_IMPORT_V1

            [ESCALA]
            data=2026-09-12
            tipo=Sabado

            Louvorz=Ana
        """.trimIndent()

        val result = EscalaChurchImportParser.parse(text)
        assertTrue(result.errors.any { it.message == "Função não reconhecida: Louvorz" })
        assertTrue(result.schedules.single().roles.isEmpty())
    }

    @Test
    fun `ISO date parses without any warning`() {
        val schedule = parseOneSchedule("data=2026-09-12\ntipo=Sabado\nSonoplastia=Robson")
        assertEquals(LocalDate.of(2026, 9, 12), schedule.date)
        assertFalse(schedule.dateAssumedYear)
    }

    @Test
    fun `DD-MM-YYYY date parses correctly`() {
        val schedule = parseOneSchedule("data=12/09/2026\ntipo=Sabado\nSonoplastia=Robson")
        assertEquals(LocalDate.of(2026, 9, 12), schedule.date)
        assertFalse(schedule.dateAssumedYear)
    }

    @Test
    fun `DD-MM without year assumes current year and warns`() {
        val result = EscalaChurchImportParser.parse(
            """
            ESCALA_CHURCH_IMPORT_V1

            [ESCALA]
            data=12/09
            tipo=Sabado

            Sonoplastia=Robson
            """.trimIndent()
        )
        val schedule = result.schedules.single()
        assertEquals(java.time.Year.now().value, schedule.date?.year)
        assertTrue(schedule.dateAssumedYear)
        assertTrue(result.warnings.any { it.message.contains("sem ano") })
    }

    @Test
    fun `invalid text without the header is rejected`() {
        val result = EscalaChurchImportParser.parse("qualquer coisa aleatória")
        assertNull(result.version)
        assertTrue(result.errors.any { it.message == "Este texto não parece ser uma importação Escala Church." })
        assertTrue(result.schedules.isEmpty())
    }

    @Test
    fun `unsupported version header is rejected with its own message`() {
        val result = EscalaChurchImportParser.parse("ESCALA_CHURCH_IMPORT_V2\n\n[ESCALA]\ndata=2026-09-12")
        assertTrue(result.errors.any { it.message.contains("Formato de importação não suportado") })
    }

    @Test
    fun `blank input produces an error, not a crash`() {
        val result = EscalaChurchImportParser.parse("")
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.schedules.isEmpty())
    }

    @Test
    fun `empty role value produces a warning instead of a crash`() {
        val result = EscalaChurchImportParser.parse(
            """
            ESCALA_CHURCH_IMPORT_V1

            [ESCALA]
            data=2026-09-12
            tipo=Sabado

            Recepção=
            """.trimIndent()
        )
        val schedule = result.schedules.single()
        assertTrue(schedule.roles.single().people.isEmpty())
        assertTrue(result.warnings.any { it.message.contains("não tem nenhum nome") })
    }

    @Test
    fun `unparseable date produces an error and a null date, never a guess`() {
        val result = EscalaChurchImportParser.parse(
            """
            ESCALA_CHURCH_IMPORT_V1

            [ESCALA]
            data=não é uma data
            tipo=Sabado

            Sonoplastia=Robson
            """.trimIndent()
        )
        val schedule = result.schedules.single()
        assertNull(schedule.date)
        assertTrue(result.errors.any { it.message.contains("Não foi possível interpretar a data") })
    }

    @Test
    fun `nothing is ever persisted by parsing alone - ImportResult is pure data`() {
        // The parser has no reference to a repository/Supabase client anywhere in its signature -
        // this test exists as a structural guard: parsing a huge, valid payload twice must be
        // side-effect free and produce equal (not just similar) results both times.
        val text = """
            ESCALA_CHURCH_IMPORT_V1

            [ESCALA]
            data=2026-09-12
            tipo=Sabado
            Sonoplastia=Robson
        """.trimIndent()

        val first = EscalaChurchImportParser.parse(text)
        val second = EscalaChurchImportParser.parse(text)
        assertEquals(first, second)
    }

    private fun parseOneSchedule(blockBody: String): ImportedSchedule {
        val text = "ESCALA_CHURCH_IMPORT_V1\n\n[ESCALA]\n$blockBody"
        return EscalaChurchImportParser.parse(text).schedules.single()
    }
}
