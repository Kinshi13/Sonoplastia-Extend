package com.escalachurch.app.domain.importer

import java.time.LocalDate

/** One `Função=Nome1|Nome2` line, after alias resolution. */
data class ImportedRole(
    /** Canonical role name (e.g. "Sonoplastia"), after [RoleAliasResolver] - never the raw text
     *  the user typed (e.g. "Som"), so downstream code never has to re-normalize it. */
    val roleName: String,
    /** Raw role text exactly as typed, kept only for error messages / "unknown role" reporting. */
    val rawRoleText: String,
    val people: List<ImportedPerson>
)

/** One name inside a `Função=Nome1|Nome2` line. Matching against the church's people bank happens
 *  later (see NameMatcher) - the parser itself never touches the network/database. */
data class ImportedPerson(val rawName: String)

/** One `[ESCALA] ... ` block, fully parsed but not yet saved. */
data class ImportedSchedule(
    val date: LocalDate?,
    /** True when [date] was resolved from a `DD/MM` (no year) line - the caller should surface a
     *  warning asking the admin to confirm the assumed year (Bloco B7: "nunca decidir
     *  silenciosamente se houver ambiguidade"). */
    val dateAssumedYear: Boolean,
    /** Raw `data=...` text, kept for display when [date] is null (couldn't be parsed at all). */
    val rawDate: String,
    val type: String,
    val startTime: String?,
    val roles: List<ImportedRole>
)

data class ImportWarning(val message: String)
data class ImportError(val message: String)

/** Result of parsing one `ESCALA_CHURCH_IMPORT_V1` payload. [schedules] may be non-empty even when
 *  [errors] is non-empty - each schedule block is parsed independently, so one bad block doesn't
 *  throw away the good ones. The caller (the preview screen) decides whether to still show a
 *  schedule that has an error attached to it. Nothing here is ever persisted - see
 *  EscalaChurchImportParser's own doc comment. */
data class ImportResult(
    val version: String?,
    val schedules: List<ImportedSchedule>,
    val warnings: List<ImportWarning>,
    val errors: List<ImportError>
) {
    val isSupportedVersion: Boolean get() = version == EscalaChurchImportParser.SUPPORTED_VERSION
}
