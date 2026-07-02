package com.escalachurch.app.domain.util

import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.UserClass
import java.time.format.DateTimeFormatter
import java.util.Locale

private val friendlyDate = DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale("pt", "BR"))

/**
 * Compares two versions of an official [ScaleItem] and reports which [UserClass]es were
 * affected, so the caller (GeneralScaleRepository) can write a [com.escalachurch.app.domain.model.ChangeLogEntry]
 * and notify only the relevant members - e.g. changing Sonoplastia notifies sonoplastas, not cantores.
 */
object ChangeDetector {

    fun affectedClasses(before: ScaleItem?, after: ScaleItem): Set<UserClass> {
        if (before == null) {
            // Brand-new scale: notify every class that has an assignment.
            return after.assignedRolesByClass().keys
        }
        return buildSet {
            if (before.soundPerson != after.soundPerson) add(UserClass.SONOPLASTA)
            if (before.conductingPerson != after.conductingPerson) add(UserClass.REGENTE)
            if (before.musicalMessagePerson != after.musicalMessagePerson) add(UserClass.CANTOR)
            if (before.preachingPerson != after.preachingPerson) add(UserClass.PREGADOR)
            if (before.receptionPerson != after.receptionPerson) add(UserClass.RECEPCIONISTA)
        }
    }

    fun summaryMessage(scale: ScaleItem, affected: Set<UserClass>): String {
        val classNames = affected.joinToString(", ") { it.label }
        val dateLabel = friendlyDate.format(scale.date)
        return "Houve uma atualização em $classNames para ${scale.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("pt", "BR"))}, $dateLabel."
    }
}
