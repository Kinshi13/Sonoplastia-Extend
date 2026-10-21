package com.escalachurch.app.domain.importer

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportRpcPayloadBuilderTest {

    private val readySchedule = CanonicalSchedule(
        title = "Culto de domingo",
        date = "2026-11-15",
        startTime = "19:00",
        type = "Culto de domingo",
        isSpecialEvent = true,
        notes = "Observação",
        roles = listOf(
            CanonicalRole(
                roleId = "role-sound",
                rawRoleName = "Som",
                normalizedRoleName = RoleAliasResolver.SONOPLASTIA,
                position = 0,
                participants = listOf(CanonicalParticipant(personId = "person-joao", rawName = "João", resolvedName = "João Batista", status = ParticipantStatus.CONFIRMED))
            ),
            CanonicalRole(
                roleId = "role-louvor",
                rawRoleName = "Louvor",
                normalizedRoleName = RoleAliasResolver.REGENCIA,
                position = 1,
                participants = listOf(
                    CanonicalParticipant(rawName = "Ana", status = ParticipantStatus.UNRESOLVED),
                    CanonicalParticipant(personId = "person-pedro", rawName = "Pedro", resolvedName = "Pedro Souza", status = ParticipantStatus.CONFIRMED)
                )
            )
        )
    )

    @Test
    fun `schedule payload carries every plain field`() {
        val payload = ImportRpcPayloadBuilder.buildSchedulePayload(readySchedule)
        assertEquals("2026-11-15", payload["date"]!!.jsonPrimitive.content)
        assertEquals("19:00", payload["start_time"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, payload["end_time"])
        assertEquals("Culto de domingo", payload["title"]!!.jsonPrimitive.content)
        assertEquals("Observação", payload["notes"]!!.jsonPrimitive.content)
        assertEquals(true, payload["is_special_event"]!!.jsonPrimitive.boolean)
        assertEquals(false, payload["is_temporary"]!!.jsonPrimitive.boolean)
        assertEquals("OFFICIAL", payload["source_type"]!!.jsonPrimitive.content)
    }

    @Test
    fun `schedule payload projects each role onto its legacy column, multiple names joined with e`() {
        val payload = ImportRpcPayloadBuilder.buildSchedulePayload(readySchedule)
        assertEquals("João Batista", payload["sound_person"]!!.jsonPrimitive.content)
        assertEquals("Ana e Pedro Souza", payload["conducting_person"]?.jsonPrimitive?.content ?: "")
    }

    @Test
    fun `assignments payload has one entry per participant across every role, position increasing`() {
        val payload = ImportRpcPayloadBuilder.buildAssignmentsPayload(readySchedule)
        assertEquals(3, payload.jsonArray.size)
        val positions = payload.jsonArray.map { it.jsonObject["position"]!!.jsonPrimitive.int }
        assertEquals(listOf(0, 1, 2), positions)
    }

    @Test
    fun `a confirmed participant gets a real person_id and no custom_person_name`() {
        val payload = ImportRpcPayloadBuilder.buildAssignmentsPayload(readySchedule)
        val joao = payload.jsonArray.first { it.jsonObject["person_name_snapshot"]!!.jsonPrimitive.content == "João Batista" }.jsonObject
        assertEquals("person-joao", joao["person_id"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, joao["custom_person_name"])
    }

    @Test
    fun `an unresolved participant gets a custom_person_name and a null person_id`() {
        val payload = ImportRpcPayloadBuilder.buildAssignmentsPayload(readySchedule)
        val ana = payload.jsonArray.first { it.jsonObject["person_name_snapshot"]!!.jsonPrimitive.content == "Ana" }.jsonObject
        assertEquals(JsonNull, ana["person_id"])
        assertEquals("Ana", ana["custom_person_name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `every assignment carries the role's real role_id, never null`() {
        val payload = ImportRpcPayloadBuilder.buildAssignmentsPayload(readySchedule)
        assertTrue(payload.jsonArray.all { it.jsonObject["role_id"]!!.jsonPrimitive.content.isNotBlank() })
    }

    @Test
    fun `building a payload for a schedule that is not ready to publish throws, never silently proceeds`() {
        val notReady = readySchedule.copy(pendingFields = listOf("date"))
        assertThrows(IllegalArgumentException::class.java) { ImportRpcPayloadBuilder.buildSchedulePayload(notReady) }
        assertThrows(IllegalArgumentException::class.java) { ImportRpcPayloadBuilder.buildAssignmentsPayload(notReady) }
    }
}
