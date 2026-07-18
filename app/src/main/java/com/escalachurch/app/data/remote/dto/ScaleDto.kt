package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.domain.model.ProgramType
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.SourceType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

/**
 * Postgres-safe mirror of [ScaleItem] for the `scales` table - dates/times are stored as ISO
 * strings (same convention the old Room [com.escalachurch.app.data.local.converter.Converters]
 * used), snake_case column names via [SerialName].
 */
@Serializable
data class ScaleDto(
    val id: String? = null,
    // The backend is multi-tenant now (church_id is a required column) - defaulted here rather
    // than threaded through every call site, since this build only ever writes its own church's
    // row anyway (see BuildConfig.CHURCH_ID / local.properties).
    @SerialName("church_id") val churchId: String = BuildConfig.CHURCH_ID,
    val date: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String? = null,
    val type: String = ProgramType.COMMON_SCALE.name,
    val title: String = "",
    @SerialName("reception_person") val receptionPerson: String = "",
    @SerialName("sound_person") val soundPerson: String = "",
    @SerialName("preaching_person") val preachingPerson: String = "",
    @SerialName("conducting_person") val conductingPerson: String = "",
    @SerialName("musical_message_person") val musicalMessagePerson: String = "",
    val notes: String = "",
    @SerialName("is_special_event") val isSpecialEvent: Boolean = false,
    @SerialName("source_type") val sourceType: String = SourceType.OFFICIAL.name,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L
)

fun ScaleItem.toDto(churchId: String = BuildConfig.CHURCH_ID) = ScaleDto(
    id = id.ifBlank { null },
    churchId = churchId,
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

fun ScaleDto.toScaleItem(): ScaleItem? { return ScaleItem(
    id = id ?: return null,
    date = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null,
    startTime = runCatching { LocalTime.parse(startTime) }.getOrNull() ?: return null,
    endTime = endTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    type = runCatching { ProgramType.valueOf(type) }.getOrDefault(ProgramType.COMMON_SCALE),
    title = title,
    receptionPerson = receptionPerson,
    soundPerson = soundPerson,
    preachingPerson = preachingPerson,
    conductingPerson = conductingPerson,
    musicalMessagePerson = musicalMessagePerson,
    notes = notes,
    isSpecialEvent = isSpecialEvent,
    sourceType = runCatching { SourceType.valueOf(sourceType) }.getOrDefault(SourceType.OFFICIAL),
    createdAt = createdAt,
    updatedAt = updatedAt
) }
