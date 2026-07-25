package com.escalachurch.app.share

/**
 * Android replica of the web's `buildScheduleShareMessage` ("Compartilhar escala"). Falls back to
 * "Próxima programação" when [scheduleTitle] is blank, and simply omits [timeLabel] when it's
 * blank - never prints an empty/"null" field. Requires [churchName]/[publicUrl]/[churchCode]
 * explicitly, same convention as [buildChurchShareMessage]/[buildAnnouncementShareMessage].
 *
 * Pure - see ScheduleShareMessageBuilderTest.
 */
fun buildScheduleShareMessage(
    churchName: String,
    churchCode: String,
    publicUrl: String,
    scheduleTitle: String,
    dateLabel: String,
    timeLabel: String
): String = buildString {
    append("📅 Escala da ").append(churchName)
    appendLine()
    appendLine()
    append(scheduleTitle.ifBlank { "Próxima programação" })
    appendLine()
    append(if (timeLabel.isBlank()) dateLabel else "$dateLabel — $timeLabel")
    appendLine()
    appendLine()
    append("Confira a escala completa, anúncios e demais informações:")
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
