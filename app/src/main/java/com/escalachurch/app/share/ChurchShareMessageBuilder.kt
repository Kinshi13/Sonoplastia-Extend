package com.escalachurch.app.share

/**
 * Fase 11.10 - the default share text for "Compartilhar acesso da igreja", matching the phase's
 * own template. [nextScaleSummary] (e.g. "Culto de Domingo · 15/09 · 19:00"), when given, is
 * inserted right after the link so the recipient sees what's coming up without opening it.
 *
 * Only ever takes [churchName]/[churchSlug]/[link] - never a church id, token, or any internal
 * identifier (Bloco: "nunca compartilhar church_id, JWT, tokens, IDs internos").
 *
 * No string-resource/i18n system exists anywhere in this app today (checked res/values/strings.xml -
 * only app_name is there; every other screen's text in the whole codebase is a hardcoded Kotlin
 * literal) - this follows that same established convention rather than introducing a new one.
 *
 * Pure - see ChurchShareMessageBuilderTest.
 */
fun buildChurchShareMessage(
    churchName: String,
    churchSlug: String,
    link: String,
    nextScaleSummary: String? = null
): String = buildString {
    append("📅 Próxima programação da ").append(churchName.ifBlank { "nossa igreja" })
    appendLine()
    appendLine()
    append("Veja a escala completa, anúncios e programação acessando:")
    appendLine()
    appendLine()
    append(link)
    appendLine()
    if (!nextScaleSummary.isNullOrBlank()) {
        appendLine()
        append(nextScaleSummary)
        appendLine()
    }
    appendLine()
    append("Código da igreja:")
    appendLine()
    appendLine()
    append(churchSlug.uppercase())
    appendLine()
    appendLine()
    append("Baixe também o aplicativo Escala Church.")
    appendLine()
    appendLine()
    append("Mensagem enviada automaticamente pelo Escala Church.")
}
