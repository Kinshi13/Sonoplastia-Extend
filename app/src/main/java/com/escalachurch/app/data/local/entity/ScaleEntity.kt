package com.escalachurch.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.escalachurch.app.domain.model.ProgramType
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.SourceType
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "scales")
data class ScaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime?,
    val type: ProgramType,
    val title: String,
    val receptionPerson: String,
    val soundPerson: String,
    val preachingPerson: String,
    val conductingPerson: String,
    val musicalMessagePerson: String,
    val notes: String,
    val isSpecialEvent: Boolean,
    val sourceType: SourceType,
    val createdAt: Long,
    val updatedAt: Long
)

fun ScaleEntity.toDomain() = ScaleItem(
    id = id,
    date = date,
    startTime = startTime,
    endTime = endTime,
    type = type,
    title = title,
    receptionPerson = receptionPerson,
    soundPerson = soundPerson,
    preachingPerson = preachingPerson,
    conductingPerson = conductingPerson,
    musicalMessagePerson = musicalMessagePerson,
    notes = notes,
    isSpecialEvent = isSpecialEvent,
    sourceType = sourceType,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ScaleItem.toEntity() = ScaleEntity(
    id = id,
    date = date,
    startTime = startTime,
    endTime = endTime,
    type = type,
    title = title,
    receptionPerson = receptionPerson,
    soundPerson = soundPerson,
    preachingPerson = preachingPerson,
    conductingPerson = conductingPerson,
    musicalMessagePerson = musicalMessagePerson,
    notes = notes,
    isSpecialEvent = isSpecialEvent,
    sourceType = sourceType,
    createdAt = createdAt,
    updatedAt = updatedAt
)
