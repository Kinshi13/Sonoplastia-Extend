package com.escalachurch.app.domain.util

import com.escalachurch.app.domain.model.ScaleItem

/** Which of a [ScaleItem]'s functions matched the logged-in user's name, if any. */
data class ScaleMatch(val scale: ScaleItem, val roles: List<String>)

/**
 * Finds whether [myName] is assigned to any function of [scale]. The comparison is
 * case-insensitive and forgiving of partial names (e.g. "Fernanda" matches
 * "Fernanda Dias"), since role fields are free text typed by whoever built the scale.
 */
object ReminderMatcher {

    fun match(scale: ScaleItem, myName: String): ScaleMatch? {
        val name = myName.trim()
        if (name.isEmpty()) return null

        val roles = buildList {
            if (namesOverlap(scale.receptionPerson, name)) add("Recepção")
            if (namesOverlap(scale.soundPerson, name)) add("Sonoplastia")
            if (namesOverlap(scale.preachingPerson, name)) add("Pregação")
            if (namesOverlap(scale.conductingPerson, name)) add("Regência")
            if (namesOverlap(scale.musicalMessagePerson, name)) add("Mensagem musical")
        }
        return if (roles.isEmpty()) null else ScaleMatch(scale, roles)
    }

    private fun namesOverlap(roleField: String, myName: String): Boolean {
        val field = roleField.trim()
        if (field.isEmpty()) return false
        return field.contains(myName, ignoreCase = true) || myName.contains(field, ignoreCase = true)
    }
}
