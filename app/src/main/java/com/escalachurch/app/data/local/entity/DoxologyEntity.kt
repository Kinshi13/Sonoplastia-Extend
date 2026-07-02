package com.escalachurch.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.domain.model.ProgramStep
import com.escalachurch.app.domain.model.SourceType
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "doxologies")
data class DoxologyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: LocalDate,
    val startTime: LocalTime,
    val title: String,
    val notes: String,
    val sourceType: SourceType,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "program_steps", indices = [Index("doxologyId")])
data class ProgramStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val doxologyId: Long,
    val order: Int,
    val title: String,
    val description: String,
    val responsiblePerson: String,
    val estimatedDurationMinutes: Int?
)

fun ProgramStepEntity.toDomain() = ProgramStep(
    id = id,
    doxologyId = doxologyId,
    order = order,
    title = title,
    description = description,
    responsiblePerson = responsiblePerson,
    estimatedDurationMinutes = estimatedDurationMinutes
)

fun ProgramStep.toEntity(doxologyId: Long) = ProgramStepEntity(
    id = id,
    doxologyId = doxologyId,
    order = order,
    title = title,
    description = description,
    responsiblePerson = responsiblePerson,
    estimatedDurationMinutes = estimatedDurationMinutes
)

fun DoxologyEntity.toDomain(steps: List<ProgramStep>) = DoxologyItem(
    id = id,
    date = date,
    startTime = startTime,
    title = title,
    notes = notes,
    programOrder = steps.sortedBy { it.order },
    sourceType = sourceType,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DoxologyItem.toEntity() = DoxologyEntity(
    id = id,
    date = date,
    startTime = startTime,
    title = title,
    notes = notes,
    sourceType = sourceType,
    createdAt = createdAt,
    updatedAt = updatedAt
)
