package com.escalachurch.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.escalachurch.app.domain.model.CustomEvent
import com.escalachurch.app.domain.model.ProgramType
import com.escalachurch.app.domain.model.RepeatRule
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "custom_events")
data class CustomEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime?,
    val description: String,
    val eventType: ProgramType,
    val repeatRule: RepeatRule,
    val groupId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

fun CustomEventEntity.toDomain() = CustomEvent(
    id = id,
    title = title,
    date = date,
    startTime = startTime,
    endTime = endTime,
    description = description,
    eventType = eventType,
    repeatRule = repeatRule,
    groupId = groupId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomEvent.toEntity() = CustomEventEntity(
    id = id,
    title = title,
    date = date,
    startTime = startTime,
    endTime = endTime,
    description = description,
    eventType = eventType,
    repeatRule = repeatRule,
    groupId = groupId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
