package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.domain.model.ProgramType
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.SourceType
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate
import java.time.LocalTime

/**
 * Firestore-safe mirror of [ScaleItem] (Firestore's POJO mapper needs a public no-arg
 * constructor and only "plain" field types - dates/times are stored as ISO strings, same
 * convention as the old Room [com.escalachurch.app.data.local.converter.Converters]).
 */
data class ScaleDto(
    val date: String = "",
    val startTime: String = "",
    val endTime: String? = null,
    val type: String = ProgramType.COMMON_SCALE.name,
    val title: String = "",
    val receptionPerson: String = "",
    val soundPerson: String = "",
    val preachingPerson: String = "",
    val conductingPerson: String = "",
    val musicalMessagePerson: String = "",
    val notes: String = "",
    val isSpecialEvent: Boolean = false,
    val sourceType: String = SourceType.OFFICIAL.name,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

fun ScaleItem.toDto() = ScaleDto(
    date = date.toString(),
    startTime = startTime.toString(),
    endTime = endTime?.toString(),
    type = type.name,
    title = title,
    receptionPerson = receptionPerson,
    soundPerson = soundPerson,
    preachingPerson = preachingPerson,
    conductingPerson = conductingPerson,
    musicalMessagePerson = musicalMessagePerson,
    notes = notes,
    isSpecialEvent = isSpecialEvent,
    sourceType = sourceType.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DocumentSnapshot.toScaleItem(): ScaleItem? {
    val dto = toObject(ScaleDto::class.java) ?: return null
    return ScaleItem(
        id = id,
        date = runCatching { LocalDate.parse(dto.date) }.getOrNull() ?: return null,
        startTime = runCatching { LocalTime.parse(dto.startTime) }.getOrNull() ?: return null,
        endTime = dto.endTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        type = runCatching { ProgramType.valueOf(dto.type) }.getOrDefault(ProgramType.COMMON_SCALE),
        title = dto.title,
        receptionPerson = dto.receptionPerson,
        soundPerson = dto.soundPerson,
        preachingPerson = dto.preachingPerson,
        conductingPerson = dto.conductingPerson,
        musicalMessagePerson = dto.musicalMessagePerson,
        notes = dto.notes,
        isSpecialEvent = dto.isSpecialEvent,
        sourceType = runCatching { SourceType.valueOf(dto.sourceType) }.getOrDefault(SourceType.OFFICIAL),
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt
    )
}
