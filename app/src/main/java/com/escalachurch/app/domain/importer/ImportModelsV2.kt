package com.escalachurch.app.domain.importer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Fase 11.15 - canonical, interface-independent representation of one or more schedules being
 * imported. Where [ImportedSchedule]/[ImportedRole]/[ImportedPerson] (see ImportModels.kt) are
 * V1's parse result - good enough for "one person per role", not for representing six individual
 * people across three roles as six real entities - this is the shared shape Android (and, later,
 * the web) can both reason about regardless of where it came from: V1 text today, a pasted
 * ChatGPT JSON response tomorrow, a from-scratch UI builder after that.
 *
 * This file defines the model and its JSON encoding ONLY. Nothing here talks to Supabase, a
 * repository, or a ViewModel - see [com.escalachurch.app.domain.importer] V1→V2 adapter
 * ([toCanonicalV2]) and reconciliation ([ParticipantReconciler]/[RoleReconciler]) for what
 * produces and refines a [CanonicalImportResult] in practice.
 */
const val IMPORT_CONTRACT_V2 = "ESCALA_CHURCH_IMPORT_V2"

/** Where a [CanonicalSchedule]'s data came from - surfaced in the preview so "colei um texto" and
 *  "reaproveitei uma escala antiga" never look the same to the admin reviewing it. */
@Serializable
enum class ImportSource {
    /** Parsed from an `ESCALA_CHURCH_IMPORT_V1` text block (see [toCanonicalV2]). */
    @SerialName("manual_text_v1") MANUAL_TEXT_V1,
    /** Pasted directly as `ESCALA_CHURCH_IMPORT_V2` JSON (e.g. produced by hand or by an LLM
     *  following the Fase 11.13 Bloco D1 prompt) - not implemented as a UI entry point yet. */
    @SerialName("manual_json_v2") MANUAL_JSON_V2,
    /** Reserved for the future D2 integrated flow (Fase 11.13) - never set by any code today. */
    @SerialName("llm_assisted") LLM_ASSISTED,
    /** An existing schedule being duplicated/reused, not a fresh import. */
    @SerialName("reuse") REUSE
}

/** How confidently a [CanonicalParticipant] has been tied to a real [OrganizationPerson] row.
 *  Mirrors the states Fase 11.13's Bloco E/Etapa F asked for - deliberately a closed, explicit
 *  set rather than a boolean "resolved or not", since "resolved" hides whether a human or the
 *  matcher made the call. */
@Serializable
enum class ParticipantStatus {
    /** The admin (or an exact, case/accent-insensitive name match) confirmed this is a specific
     *  real person - [CanonicalParticipant.personId] is set. */
    @SerialName("confirmed") CONFIRMED,
    /** [NameMatcher] found exactly one plausible match - shown, never auto-applied. */
    @SerialName("suggested") SUGGESTED,
    /** More than one known person is an equally plausible match (e.g. "João" against three
     *  different "João ..." people) - [CanonicalParticipant.candidateNames] lists all of them;
     *  the admin must pick, the system never does. */
    @SerialName("conflict") CONFLICT,
    /** No known person matches closely enough - [rawName] stands alone, a real schedule row can
     *  still be created with it as free text (`custom_person_name`, mirroring the web's own
     *  [OrganizationPerson]-or-free-text model), never silently cadastrada. */
    @SerialName("unresolved") UNRESOLVED,
    /** The admin explicitly chose "manter nome temporário" over every suggestion offered - a
     *  distinct state from [UNRESOLVED] (which just hasn't been looked at yet) so the preview can
     *  tell "ainda não revisado" apart from "revisado, decidiu não vincular". */
    @SerialName("temporary_name") TEMPORARY_NAME
}

/** One person inside one [CanonicalRole] - never collapsed into a joined string. Fase 11.15's
 *  whole point: "Ana, Pedro e Maria" in "Louvor" is three of these, not one. */
@Serializable
data class CanonicalParticipant(
    /** Set only once [status] is [ParticipantStatus.CONFIRMED] - a real `organization_people.id`.
     *  Never invented; only ever copied from a real lookup result (see [ParticipantReconciler]). */
    val personId: String? = null,
    /** Exactly what the source said, untouched - always kept, even once resolved, so the admin
     *  can always see what the import actually read. */
    val rawName: String,
    /** The name to display once [status] is [ParticipantStatus.CONFIRMED]/[SUGGESTED] - the known
     *  person's own name, which may differ in spelling/accents from [rawName]. */
    val resolvedName: String? = null,
    val status: ParticipantStatus = ParticipantStatus.UNRESOLVED,
    /** Populated for [ParticipantStatus.SUGGESTED] (one name) and [ParticipantStatus.CONFLICT]
     *  (more than one) - empty otherwise. */
    val candidateNames: List<String> = emptyList(),
    val notes: String? = null
)

/** One function/role inside one [CanonicalSchedule], with every assigned person preserved as its
 *  own [CanonicalParticipant] in [participants] - never joined into a single display string as
 *  the only representation (that join still happens, but only as a legacy-compatibility
 *  *projection*, not as the source of truth - see ImportV1ToV2Adapter). */
@Serializable
data class CanonicalRole(
    /** A real `organization_roles.id`, once [RoleReconciler] finds one - null means "pendente de
     *  conciliação", never an invented id. */
    val roleId: String? = null,
    /** Exactly what the source called this role ("Som", "Louvor especial", ...). */
    val rawRoleName: String,
    /** [RoleAliasResolver]'s canonical name when [rawRoleName] matches a known alias, or
     *  [rawRoleName] itself (title-cased) when it doesn't - what the preview actually displays as
     *  the role's name, distinct from the raw text kept for audit/debugging. */
    val normalizedRoleName: String,
    /** Display/save order among this schedule's roles - preserved from the source, never
     *  re-sorted alphabetically. */
    val position: Int = 0,
    val participants: List<CanonicalParticipant> = emptyList()
)

/** One schedule being imported, fully structured, still unsaved. [localId] is a client-only
 *  identifier (stable for the lifetime of one import session) - never a real `scales.id`; it
 *  exists so the preview/reconciliation/persistence layers can all refer to "this one" without
 *  waiting for a database round-trip to get a real id. */
@Serializable
data class CanonicalSchedule(
    val localId: String = UUID.randomUUID().toString(),
    val title: String = "",
    /** Exactly what the source said for the date, even when it couldn't be parsed - never
     *  discarded just because [date] below is null. */
    val rawDate: String = "",
    /** ISO `yyyy-MM-dd`, or null when the date is still unresolved - a null [date] is NOT the
     *  same as "no date was ever given" (see [rawDate]) and must never be silently defaulted to
     *  today or any other guessed value (Etapa B: "não preencher silenciosamente"). */
    val date: String? = null,
    /** True when [date] was resolved from a `DD/MM` (no year) source - the admin must confirm it
     *  before publishing, same rule V1 already enforces. */
    val dateAssumedYear: Boolean = false,
    /** `HH:MM`, or null - never defaulted to a guessed time. */
    val startTime: String? = null,
    val endTime: String? = null,
    val type: String = "",
    val isSpecialEvent: Boolean = false,
    val notes: String = "",
    val source: ImportSource = ImportSource.MANUAL_TEXT_V1,
    val roles: List<CanonicalRole> = emptyList(),
    /** Non-blocking - informational only (Etapa B/F: "não bloquear por alertas meramente
     *  informativos"). */
    val warnings: List<String> = emptyList(),
    /** Blocking - a schedule with a non-empty [errors] must not be published as-is. */
    val errors: List<String> = emptyList(),
    /** Field names (e.g. `"date"`, `"startTime"`) that are still missing/unresolved and must be
     *  filled before this schedule can be published - distinct from [errors]: a pending field is
     *  "not decided yet", not "something is wrong". Computed by [toCanonicalV2]/reconciliation,
     *  recomputed as the admin edits the draft. */
    val pendingFields: List<String> = emptyList()
) {
    /** A schedule is ready to publish only when it has no blocking [errors] and nothing left
     *  [pendingFields] - mirrors Etapa E's "sucesso não pode ser afirmado com algo essencial
     *  faltando", applied before persistence is even attempted. */
    val isReadyToPublish: Boolean get() = errors.isEmpty() && pendingFields.isEmpty()
}

/** Result of interpreting one import operation into the canonical V2 model - whether it came from
 *  V1 text, pasted V2 JSON, or (later) an LLM. [schedules] may be non-empty even when
 *  [globalErrors] is non-empty, same "one bad block doesn't throw away the good ones" rule V1
 *  already follows. */
@Serializable
data class CanonicalImportResult(
    val contractVersion: String = IMPORT_CONTRACT_V2,
    val schedules: List<CanonicalSchedule> = emptyList(),
    /** V1's parser reports warnings/errors for the whole document, not per schedule block (see
     *  [toCanonicalV2]'s own doc comment) - kept here, at this level, rather than invented a false
     *  per-schedule attribution the source data doesn't actually support. */
    val globalWarnings: List<String> = emptyList(),
    val globalErrors: List<String> = emptyList()
) {
    val isSupportedVersion: Boolean get() = contractVersion == IMPORT_CONTRACT_V2
}

private val importJson = Json { ignoreUnknownKeys = true; prettyPrint = true }

/** Etapa B: "preferir uma representação JSON estruturada" - this is that representation,
 *  rendered. Round-trips with [parseCanonicalImportJson]. */
fun CanonicalImportResult.toJson(): String = importJson.encodeToString(this)

/** Parses a pasted `ESCALA_CHURCH_IMPORT_V2` JSON payload. Never throws - an invalid/foreign
 *  payload becomes a [CanonicalImportResult] with [CanonicalImportResult.globalErrors] set and no
 *  schedules, same "never crash on bad input" rule the V1 parser already follows. Unknown JSON
 *  keys are ignored (Etapa B: "compatibilidade futura"), not treated as errors. */
fun parseCanonicalImportJson(rawJson: String): CanonicalImportResult {
    if (rawJson.isBlank()) return CanonicalImportResult(schedules = emptyList(), globalErrors = listOf("O JSON está vazio."))
    val parsed = runCatching { importJson.decodeFromString<CanonicalImportResult>(rawJson) }
        .getOrElse { return CanonicalImportResult(schedules = emptyList(), globalErrors = listOf("JSON inválido ou fora do contrato $IMPORT_CONTRACT_V2.")) }
    if (!parsed.isSupportedVersion) {
        return parsed.copy(globalErrors = parsed.globalErrors + "Versão de contrato não suportada: \"${parsed.contractVersion}\".")
    }
    return parsed
}
