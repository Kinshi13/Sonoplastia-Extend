package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.domain.model.ProgramStep
import com.escalachurch.app.domain.model.SourceType
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate
import java.time.LocalTime

data class ProgramStepDto(
    val order: Int = 0,
    val title: String = "",
    val description: String = "",
    val responsiblePerson: String = "",
    val estimatedDurationMinutes: Int? = null
)

fun ProgramStep.toDto() = ProgramStepDto(order, title, description, responsiblePerson, estimatedDurationMinutes)

fun ProgramStepDto.toDomain() = ProgramStep(order, title, description, responsiblePerson, estimatedDurationMinutes)

/** Firestore-safe mirror of [DoxologyItem]; steps are embedded (no separate collection/join needed). */
data class DoxologyDto(
    val date: String = "",
    val startTime: String = "",
    val title: String = "",
    val notes: String = "",
    val programOrder: List<ProgramStepDto> = emptyList(),
    val sourceType: String = SourceType.OFFICIAL.name,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

fun DoxologyItem.toDto() = DoxologyDto(
    date = date.toString(),
    startTime = startTime.toString(),
    title = title,
    notes = notes,
    programOrder = programOrder.map { it.toDto() },
    sourceType = sourceType.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DocumentSnapshot.toDoxologyItem(): DoxologyItem? {
    val dto = toObject(DoxologyDto::class.java) ?: return null
    return DoxologyItem(
        id = id,
        date = runCatching { LocalDate.parse(dto.date) }.getOrNull() ?: return null,
        startTime = runCatching { LocalTime.parse(dto.startTime) }.getOrNull() ?: return null,
        title = dto.title,
        notes = dto.notes,
        programOrder = dto.programOrder.map { it.toDomain() },
        sourceType = runCatching { SourceType.valueOf(dto.sourceType) }.getOrDefault(SourceType.OFFICIAL),
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt
    )
}
