package com.escalachurch.app.domain.importer

import com.escalachurch.app.domain.model.OrganizationPerson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParticipantReconcilerTest {

    private val joaoPedro = OrganizationPerson(id = "p1", fullName = "João Pedro Almeida")
    private val joaoCarlos = OrganizationPerson(id = "p2", fullName = "João Carlos Ferreira")
    private val ana = OrganizationPerson(id = "p3", fullName = "Ana Paula")

    private fun resultWith(vararg rawNames: String): CanonicalImportResult = CanonicalImportResult(
        schedules = listOf(
            CanonicalSchedule(
                roles = listOf(
                    CanonicalRole(
                        rawRoleName = "Louvor",
                        normalizedRoleName = RoleAliasResolver.REGENCIA,
                        participants = rawNames.map { CanonicalParticipant(rawName = it) }
                    )
                )
            )
        )
    )

    @Test
    fun `an exact name becomes CONFIRMED with the real person id`() {
        val reconciled = ParticipantReconciler.reconcile(resultWith("Ana Paula"), listOf(ana))
        val participant = reconciled.schedules.single().roles.single().participants.single()
        assertEquals(ParticipantStatus.CONFIRMED, participant.status)
        assertEquals("p3", participant.personId)
        assertEquals("Ana Paula", participant.resolvedName)
    }

    @Test
    fun `a name matching several known people becomes CONFLICT, not auto-picked`() {
        val reconciled = ParticipantReconciler.reconcile(resultWith("João"), listOf(joaoPedro, joaoCarlos, ana))
        val participant = reconciled.schedules.single().roles.single().participants.single()
        assertEquals(ParticipantStatus.CONFLICT, participant.status)
        assertNull(participant.personId)
        assertEquals(setOf("João Pedro Almeida", "João Carlos Ferreira"), participant.candidateNames.toSet())
    }

    @Test
    fun `a name with no plausible match stays UNRESOLVED`() {
        val reconciled = ParticipantReconciler.reconcile(resultWith("Zeferino"), listOf(ana, joaoPedro))
        val participant = reconciled.schedules.single().roles.single().participants.single()
        assertEquals(ParticipantStatus.UNRESOLVED, participant.status)
        assertNull(participant.personId)
    }

    @Test
    fun `an empty people bank leaves every participant untouched (not re-flagged as unresolved)`() {
        val original = resultWith("Ana Paula")
        val reconciled = ParticipantReconciler.reconcile(original, emptyList())
        assertEquals(original, reconciled)
    }

    @Test
    fun `a participant already confirmed by a human is never re-matched`() {
        val alreadyDecided = resultWith("Ana Paula").let { result ->
            result.copy(schedules = result.schedules.map { schedule ->
                schedule.copy(roles = schedule.roles.map { role ->
                    role.copy(participants = role.participants.map {
                        it.copy(personId = "manually-picked-id", status = ParticipantStatus.CONFIRMED, resolvedName = "Outra Pessoa")
                    })
                })
            })
        }
        // Even though "Ana Paula" would exact-match `ana` below, the human's earlier decision wins.
        val reconciled = ParticipantReconciler.reconcile(alreadyDecided, listOf(ana))
        val participant = reconciled.schedules.single().roles.single().participants.single()
        assertEquals("manually-picked-id", participant.personId)
        assertEquals("Outra Pessoa", participant.resolvedName)
    }

    @Test
    fun `multiple participants in the same role are reconciled independently`() {
        val reconciled = ParticipantReconciler.reconcile(resultWith("Ana Paula", "João"), listOf(ana, joaoPedro, joaoCarlos))
        val participants = reconciled.schedules.single().roles.single().participants
        assertEquals(ParticipantStatus.CONFIRMED, participants[0].status)
        assertEquals(ParticipantStatus.CONFLICT, participants[1].status)
    }
}
