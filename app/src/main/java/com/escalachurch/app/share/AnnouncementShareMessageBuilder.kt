package com.escalachurch.app.share

/**
 * Android replica of the web's `buildAnnouncementShareMessage` (Web Fase - "Compartilhamento de
 * anúncios"). Requires [title]/[churchName]/[publicUrl]/[churchCode] explicitly, same as
 * [buildChurchShareMessage] - never a fixed/default string, never a church id or admin URL.
 * [description] may be blank (not every announcement has one) and is simply omitted rather than
 * printed as an empty line.
 *
 * Pure - see AnnouncementShareMessageBuilderTest.
 */
fun buildAnnouncementShareMessage(
    title: String,
    description: String,
    churchName: String,
    churchCode: String,
    publicUrl: String
): String = buildString {
    append("📢 ").append(title)
    if (description.isNotBlank()) {
        appendLine()
        appendLine()
        append(description)
    }
    appendLine()
    appendLine()
    append("Veja todos os anúncios e a programação da ").append(churchName).append(":")
    appendLine()
    append(publicUrl)
    appendLine()
    appendLine()
    append("Código da igreja:")
    appendLine()
    append(churchCode)
    appendLine()
    appendLine()
    append("Compartilhado pelo Escala Church.")
}
