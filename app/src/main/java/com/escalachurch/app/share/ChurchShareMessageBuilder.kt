package com.escalachurch.app.share

/**
 * Fase 11.10 (correção) - the share text for "Compartilhar acesso da igreja". Requires
 * [churchName]/[churchCode]/[publicUrl] - there is no fixed/default message this can fall back to
 * (root cause of the original bug: the Bottom Sheet built its own message inline instead of always
 * routing through this function with real ViewModel state, so a stale or default value could slip
 * through). Never takes a church id, JWT, or any internal identifier.
 *
 * No string-resource/i18n system exists anywhere in this app today (checked res/values/strings.xml -
 * only app_name is there) - this follows that same established convention.
 *
 * Pure - see ChurchShareMessageBuilderTest.
 */
fun buildChurchShareMessage(churchName: String, churchCode: String, publicUrl: String): String = buildString {
    append("📅 Próxima programação da ").append(churchName)
    appendLine()
    appendLine()
    append("Acesse a escala completa, anúncios e programações da igreja:")
    appendLine()
    appendLine()
    append(publicUrl)
    appendLine()
    appendLine()
    append("🔑 Código da igreja:")
    appendLine()
    append(churchCode)
    appendLine()
    appendLine()
    append("Você também pode acompanhar pelo aplicativo Escala Church.")
    appendLine()
    appendLine()
    append("Mensagem enviada pelo Escala Church.")
}
