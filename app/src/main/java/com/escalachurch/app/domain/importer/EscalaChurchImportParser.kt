package com.escalachurch.app.domain.importer

import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter

/**
 * Parses the `ESCALA_CHURCH_IMPORT_V1` text contract (Bloco B1/B4) into structured, still-unsaved
 * [ImportedSchedule]s. Deliberately offline and LLM-free (Bloco B9/B10: "esta função NÃO deve
 * utilizar LLM... o parser deve ser independente da origem" - a future assistant could produce
 * this same text contract, but this class never calls out to one). Text in, structured data out -
 * nothing here touches the network, a ViewModel, or Supabase, and nothing is ever persisted by
 * this class - that only happens after the admin reviews the preview and taps "Criar escalas"
 * (see the import screen / repository call site).
 *
 * Kept as small, named, independently-testable functions instead of one large regex (Bloco B4).
 */
object EscalaChurchImportParser {

    const val SUPPORTED_VERSION = "ESCALA_CHURCH_IMPORT_V1"
    private const val SCHEDULE_MARKER = "[ESCALA]"

    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE
    private val dmyDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dmDateNoYear = Regex("^(\\d{1,2})/(\\d{1,2})$")

    fun parse(rawText: String): ImportResult {
        val warnings = mutableListOf<ImportWarning>()
        val errors = mutableListOf<ImportError>()

        val lines = rawText.lines().map { it.trim() }
        val firstNonBlank = lines.firstOrNull { it.isNotBlank() }

        if (firstNonBlank == null) {
            errors += ImportError("O texto está vazio.")
            return ImportResult(version = null, schedules = emptyList(), warnings = warnings, errors = errors)
        }

        if (firstNonBlank != SUPPORTED_VERSION) {
            val looksLikeOurFormat = firstNonBlank.startsWith("ESCALA_CHURCH_IMPORT")
            errors += if (looksLikeOurFormat) {
                ImportError("Formato de importação não suportado: \"$firstNonBlank\".")
            } else {
                ImportError("Este texto não parece ser uma importação Escala Church.")
            }
            return ImportResult(version = null, schedules = emptyList(), warnings = warnings, errors = errors)
        }

        val bodyLines = lines.drop(lines.indexOf(firstNonBlank) + 1)
        val blocks = splitIntoScheduleBlocks(bodyLines)
        if (blocks.isEmpty()) {
            errors += ImportError("Nenhum bloco [ESCALA] encontrado.")
        }

        val schedules = blocks.map { block -> parseScheduleBlock(block, warnings, errors) }

        return ImportResult(version = firstNonBlank, schedules = schedules, warnings = warnings, errors = errors)
    }

    /** Splits the body into one line-list per `[ESCALA]` block, dropping the marker line itself
     *  and any blank lines directly around it - blank lines between `chave=valor` lines inside a
     *  block are otherwise harmless and simply skipped when parsing fields/roles. */
    private fun splitIntoScheduleBlocks(bodyLines: List<String>): List<List<String>> {
        val blocks = mutableListOf<MutableList<String>>()
        for (line in bodyLines) {
            if (line == SCHEDULE_MARKER) {
                blocks.add(mutableListOf())
                continue
            }
            if (line.isBlank()) continue
            if (blocks.isEmpty()) continue // stray content before the first [ESCALA] - ignored, not an error
            blocks.last().add(line)
        }
        return blocks
    }

    private val fieldLine = Regex("^(data|tipo|inicio)=(.*)$")
    private val roleLine = Regex("^([^=]+)=(.*)$")

    private fun parseScheduleBlock(
        block: List<String>,
        warnings: MutableList<ImportWarning>,
        errors: MutableList<ImportError>
    ): ImportedSchedule {
        var rawDate = ""
        var type = ""
        var startTime: String? = null
        val roles = mutableListOf<ImportedRole>()

        for (line in block) {
            val field = fieldLine.find(line)
            if (field != null) {
                when (field.groupValues[1]) {
                    "data" -> rawDate = field.groupValues[2].trim()
                    "tipo" -> type = field.groupValues[2].trim()
                    "inicio" -> startTime = field.groupValues[2].trim().ifBlank { null }
                }
                continue
            }

            val role = roleLine.find(line) ?: continue // not a recognized "chave=valor" shape - ignored, not fatal
            val rawRoleText = role.groupValues[1].trim()
            val peopleText = role.groupValues[2].trim()
            if (rawRoleText.isBlank()) continue

            val canonicalRole = RoleAliasResolver.resolve(rawRoleText)
            if (canonicalRole == null) {
                errors += ImportError("Função não reconhecida: $rawRoleText")
                continue
            }

            val people = peopleText.split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { ImportedPerson(rawName = it) }

            if (people.isEmpty()) {
                warnings += ImportWarning("$canonicalRole não tem nenhum nome preenchido.")
            }

            roles += ImportedRole(roleName = canonicalRole, rawRoleText = rawRoleText, people = people)
        }

        val (date, assumedYear) = parseDate(rawDate, warnings, errors)

        if (type.isBlank()) {
            warnings += ImportWarning("Escala de ${rawDate.ifBlank { "data não informada" }} está sem \"tipo\".")
        }

        return ImportedSchedule(
            date = date,
            dateAssumedYear = assumedYear,
            rawDate = rawDate,
            type = type,
            startTime = startTime,
            roles = roles
        )
    }

    /** Bloco B7: ISO (`YYYY-MM-DD`) is the official format; `DD/MM/YYYY` and `DD/MM` (assumes the
     *  current year, flagged via the returned boolean so the caller can show a warning) are
     *  accepted as a convenience. Never silently resolves an ambiguous/unparsable date - returns
     *  null and records an error instead. */
    private fun parseDate(
        rawDate: String,
        warnings: MutableList<ImportWarning>,
        errors: MutableList<ImportError>
    ): Pair<LocalDate?, Boolean> {
        if (rawDate.isBlank()) {
            errors += ImportError("Não foi possível interpretar a data.")
            return null to false
        }

        runCatching { LocalDate.parse(rawDate, isoDate) }.getOrNull()?.let { return it to false }
        runCatching { LocalDate.parse(rawDate, dmyDate) }.getOrNull()?.let { return it to false }

        dmDateNoYear.find(rawDate)?.let { match ->
            val day = match.groupValues[1].toIntOrNull()
            val month = match.groupValues[2].toIntOrNull()
            if (day != null && month != null) {
                val assumedYear = Year.now().value
                val date = runCatching { LocalDate.of(assumedYear, month, day) }.getOrNull()
                if (date != null) {
                    warnings += ImportWarning("Data \"$rawDate\" sem ano - assumido $assumedYear. Confirme antes de importar.")
                    return date to true
                }
            }
        }

        errors += ImportError("Não foi possível interpretar a data: \"$rawDate\".")
        return null to false
    }
}
