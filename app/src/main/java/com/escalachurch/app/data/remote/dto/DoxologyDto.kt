package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.domain.model.ProgramStep
import com.escalachurch.app.domain.model.SourceType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class ProgramStepDto(
    val order: Int = 0,
    val title: String = "",
    val description: String = "",
    @SerialName("responsible_person") val responsiblePerson: String = "",
    @SerialName("estimated_duration_minutes") val estimatedDurationMinutes: Int? = null
)

fun ProgramStep.toDto() = ProgramStepDto(order, title, description, responsiblePerson, estimatedDurationMinutes)

fun ProgramStepDto.toDomain() = ProgramStep(order, title, description, responsiblePerson, estimatedDurationMinutes)

/** Postgres-safe mirror of [DoxologyItem] for the `doxologies` table; steps live in a jsonb column. */
@Serializable
data class DoxologyDto(
    val id: String? = null,
    val date: String = "",
    @SerialName("start_time") val startTime: String = "",
    val title: String = "",
    val notes: String = "",
    @SerialName("program_order") val programOrder: List<ProgramStepDto> = emptyList(),
    @SerialName("source_type") val sourceType: String = SourceType.OFFICIAL.name,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L
)

fun DoxologyItem.toDto() = DoxologyDto(
    id = id.ifBlank { null },
    date = date.toString(),
    startTime = startTime.toString(),
    title = title,
    notes = notes,
    programOrder = programOrder.map { it.toDto() },
    sourceType = sourceType.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DoxologyDto.toDoxologyItem(): DoxologyItem? { return DoxologyItem(
    id = id ?: return null,
    date = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null,
    startTime = runCatching { LocalTime.parse(startTime) }.getOrNull() ?: return null,
    title = title,
    notes = notes,
    programOrder = programOrder.map { it.toDomain() },
    sourceType = runCatching { SourceType.valueOf(sourceType) }.getOrDefault(SourceType.OFFICIAL),
    createdAt = createdAt,
    updatedAt = updatedAt
) }
