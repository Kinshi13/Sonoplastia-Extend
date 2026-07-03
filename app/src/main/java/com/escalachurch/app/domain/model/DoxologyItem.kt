package com.escalachurch.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** Represents the order of service (liturgy) for a given date. */
data class DoxologyItem(
    /** Firestore document id; empty string means "not saved yet". */
    val id: String = "",
    val date: LocalDate,
    val startTime: LocalTime,
    val title: String,
    val notes: String = "",
    val programOrder: List<ProgramStep> = emptyList(),
    val sourceType: SourceType = SourceType.OFFICIAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** A single step within a Doxology's order of service - stored embedded in the parent document. */
data class ProgramStep(
    val order: Int,
    val title: String,
    val description: String = "",
    val responsiblePerson: String = "",
    val estimatedDurationMinutes: Int? = null
)
