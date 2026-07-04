package com.escalachurch.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** Represents the order of service (liturgy) for a given date. */
data class DoxologyItem(
    /** Firestore document id; empty string means "not saved yet". */
    val id: String = "",
    val date: LocalDate,
    val startTime: LocalTime,
    /** Optional: lets a single day have multiple sessions (Escola Sabatina, Culto Divino, JA...),
     *  each with its own range, so the UI can highlight whichever one is happening now. */
    val endTime: LocalTime? = null,
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
