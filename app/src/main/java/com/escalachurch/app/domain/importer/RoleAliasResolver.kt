package com.escalachurch.app.domain.importer

/**
 * Local, offline dictionary mapping loose Portuguese function names to this app's five canonical
 * roles (the same five `scales` legacy columns every other screen already writes - see
 * ScaleItem/ScaleDto). No LLM, no network - just a fixed alias table (Bloco B5).
 *
 * A role the admin already uses on the web (organization_roles) with a custom name has no alias
 * entry here and is reported as "unknown role" (Bloco B8) rather than silently dropped or
 * invented - Bloco B5: "não criar função nova automaticamente sem confirmação".
 */
object RoleAliasResolver {

    const val RECEPCAO = "Recepção"
    const val SONOPLASTIA = "Sonoplastia"
    const val PREGACAO = "Pregação"
    const val REGENCIA = "Regência"
    const val MENSAGEM_MUSICAL = "Mensagem Musical"

    /** Every alias, lower-cased/unaccented, mapped to its canonical name. Built once, not per call. */
    private val aliasToCanonical: Map<String, String> = buildMap {
        put(RECEPCAO, RECEPCAO)
        put("Recepcionista", RECEPCAO)

        put(SONOPLASTIA, SONOPLASTIA)
        put("Som", SONOPLASTIA)
        put("Áudio", SONOPLASTIA)
        put("Audio", SONOPLASTIA)

        put(REGENCIA, REGENCIA)
        put("Louvor", REGENCIA)
        put("Regente", REGENCIA)
        put("Dirigente de louvor", REGENCIA)

        put(MENSAGEM_MUSICAL, MENSAGEM_MUSICAL)
        put("Especial", MENSAGEM_MUSICAL)
        put("Solo", MENSAGEM_MUSICAL)
        put("Louvor especial", MENSAGEM_MUSICAL)

        put(PREGACAO, PREGACAO)
        put("Palavra", PREGACAO)
        put("Pregador", PREGACAO)
    }.mapKeys { (alias, _) -> normalize(alias) }

    /** Returns the canonical role name for [rawRoleText], or null if it doesn't match any known
     *  role/alias (the caller reports that as an "unknown role" error - never guesses). */
    fun resolve(rawRoleText: String): String? = aliasToCanonical[normalize(rawRoleText)]

    /** Case/accent/whitespace-insensitive key so "Som", "SOM", " som " and "Sôm" (typo-adjacent
     *  accent) all resolve the same way, without pulling in a locale-aware collator. */
    private fun normalize(text: String): String =
        text.trim()
            .lowercase()
            .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
            .replace(Regex("\\p{Mn}+"), "")
}
