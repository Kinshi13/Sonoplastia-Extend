package com.escalachurch.app.domain.importer

/** Outcome of matching one [ImportedPerson] against the church's known people (Bloco B6). Never
 *  substitutes silently - EXACT auto-links, everything else is left for the admin to decide in
 *  the preview screen. */
sealed class NameMatch {
    /** Exact match (case/accent-insensitive) - safe to auto-link without asking. */
    data class Exact(val personName: String) : NameMatch()
    /** Close but not exact (e.g. "Robsonn" vs "Robson") - shown as a suggestion, never applied
     *  automatically. */
    data class Suggestion(val personName: String) : NameMatch()
    /** No known person is close enough - the preview offers "usar como nome avulso" /
     *  "selecionar pessoa existente" / "cadastrar pessoa". */
    data object NotFound : NameMatch()
}

/** Matches imported names against a church's existing people bank, entirely offline (no LLM, no
 *  network call here - the caller fetches the name list once and passes it in). */
object NameMatcher {

    /** Above this edit-distance-to-name-length ratio, two names are considered unrelated rather
     *  than a typo - keeps short names (e.g. "Ana" vs "Eva") from matching each other by accident. */
    private const val MAX_DISTANCE_RATIO = 0.34

    fun match(rawName: String, knownNames: List<String>): NameMatch {
        val normalizedInput = normalize(rawName)
        if (normalizedInput.isBlank() || knownNames.isEmpty()) return NameMatch.NotFound

        knownNames.firstOrNull { normalize(it) == normalizedInput }?.let { return NameMatch.Exact(it) }

        val closest = knownNames
            .map { it to levenshtein(normalizedInput, normalize(it)) }
            .minByOrNull { it.second }
            ?: return NameMatch.NotFound

        val (candidate, distance) = closest
        val maxAllowed = (maxOf(normalizedInput.length, normalize(candidate).length) * MAX_DISTANCE_RATIO)
        return if (distance in 1..maxAllowed.toInt().coerceAtLeast(1)) NameMatch.Suggestion(candidate) else NameMatch.NotFound
    }

    private fun normalize(text: String): String =
        text.trim()
            .lowercase()
            .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
            .replace(Regex("\\p{Mn}+"), "")

    /** Classic iterative Levenshtein distance - no dependency needed for a handful of short names. */
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost
                )
            }
            val tmp = previous
            previous = current
            current = tmp
        }
        return previous[b.length]
    }
}
