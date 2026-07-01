package com.escalachurch.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Represents a single church service scale (who is responsible for what).
 */
data class ScaleItem(
    val id: Long = 0L,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime? = null,
    val type: ProgramType = ProgramType.COMMON_SCALE,
    val title: String,
    val receptionPerson: String = "",
    val soundPerson: String = "",
    val preachingPerson: String = "",
    val conductingPerson: String = "",
    val musicalMessagePerson: String = "",
    val notes: String = "",
    val isSpecialEvent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Whether at least one function has a responsible person assigned. */
    fun hasAnyAssignment(): Boolean =
        listOf(receptionPerson, soundPerson, preachingPerson, conductingPerson, musicalMessagePerson)
            .any { it.isNotBlank() }
}
