package com.escalachurch.app.domain.importer

import com.escalachurch.app.domain.model.OrganizationPerson

/**
 * Fase 11.15 (Etapa F) - annotates every [CanonicalParticipant] in a [CanonicalImportResult]
 * against a church's real people bank, using the same offline [NameMatcher] V1 already had but
 * never actually wired up (see Fase 11.13's audit finding). Pure function: the caller fetches
 * [knownPeople] once (one [com.escalachurch.app.data.repository.OrganizationPersonRepository]
 * read, already scoped to the active church) and passes it in - this never touches the network,
 * never crosses church boundaries (the caller is responsible for only ever passing people from
 * the same church the import targets), and never creates a person. A name with no good match
 * stays [ParticipantStatus.UNRESOLVED], never silently linked to someone else's record.
 */
object ParticipantReconciler {

    fun reconcile(result: CanonicalImportResult, knownPeople: List<OrganizationPerson>): CanonicalImportResult {
        if (knownPeople.isEmpty()) return result
        val names = knownPeople.map { it.nameForMatching }
        val personByName = knownPeople.associateBy { it.nameForMatching }

        return result.copy(
            schedules = result.schedules.map { schedule ->
                schedule.copy(roles = schedule.roles.map { role ->
                    role.copy(participants = role.participants.map { reconcileOne(it, names, personByName) })
                })
            }
        )
    }

    private fun reconcileOne(
        participant: CanonicalParticipant,
        names: List<String>,
        personByName: Map<String, OrganizationPerson>
    ): CanonicalParticipant {
        // Already decided by a human (CONFIRMED with a real id, or explicitly kept TEMPORARY_NAME)
        // - never re-run the matcher over a decision that was already made.
        if (participant.status == ParticipantStatus.CONFIRMED || participant.status == ParticipantStatus.TEMPORARY_NAME) {
            return participant
        }
        if (participant.rawName.isBlank()) return participant

        return when (val match = NameMatcher.match(participant.rawName, names)) {
            is NameMatch.Exact -> participant.copy(
                personId = personByName[match.personName]?.id,
                resolvedName = match.personName,
                status = ParticipantStatus.CONFIRMED,
                candidateNames = emptyList()
            )
            is NameMatch.Suggestion -> participant.copy(
                personId = null,
                resolvedName = match.personName,
                status = ParticipantStatus.SUGGESTED,
                candidateNames = listOf(match.personName)
            )
            is NameMatch.Ambiguous -> participant.copy(
                personId = null,
                resolvedName = null,
                status = ParticipantStatus.CONFLICT,
                candidateNames = match.candidates
            )
            NameMatch.NotFound -> participant.copy(
                personId = null,
                resolvedName = null,
                status = ParticipantStatus.UNRESOLVED,
                candidateNames = emptyList()
            )
        }
    }
}
