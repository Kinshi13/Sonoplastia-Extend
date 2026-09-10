package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.domain.model.WorshipSong
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

/** Mirrors `public.worship_songs` (supabase/migrations/014-016) - same table the web site's
 *  "Música e Louvor" already uses. No Android-only columns are added here. */
@Serializable
data class WorshipSongDto(
    val id: String? = null,
    @SerialName("church_id") val churchId: String = BuildConfig.CHURCH_ID,
    @SerialName("schedule_id") val scheduleId: String? = null,
    @SerialName("program_date") val programDate: String? = null,
    @SerialName("program_type") val programType: String? = null,
    val title: String = "",
    val artist: String = "",
    @SerialName("youtube_url") val youtubeUrl: String = "",
    @SerialName("youtube_video_id") val youtubeVideoId: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    @SerialName("moment_label") val momentLabel: String = "",
    val notes: String = "",
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("is_published") val isPublished: Boolean = true,
    @SerialName("is_daily_recommendation") val isDailyRecommendation: Boolean = false,
    @SerialName("recommendation_date") val recommendationDate: String? = null,
    @SerialName("recommendation_message") val recommendationMessage: String? = null,
    @SerialName("notification_enabled") val notificationEnabled: Boolean = false,
    @SerialName("notification_time") val notificationTime: String? = null,
    @SerialName("notification_title") val notificationTitle: String? = null,
    @SerialName("notification_body") val notificationBody: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L
)

fun WorshipSong.toDto(churchId: String = BuildConfig.CHURCH_ID) = WorshipSongDto(
    id = id.ifBlank { null },
    churchId = churchId,
    scheduleId = scheduleId,
    programDate = programDate?.toString(),
    programType = programType,
    title = title,
    artist = artist,
    youtubeUrl = youtubeUrl,
    youtubeVideoId = youtubeVideoId,
    thumbnailUrl = thumbnailUrl,
    momentLabel = momentLabel,
    notes = notes,
    orderIndex = orderIndex,
    isPublished = isPublished,
    isDailyRecommendation = isDailyRecommendation,
    recommendationDate = recommendationDate?.toString(),
    recommendationMessage = recommendationMessage,
    notificationEnabled = notificationEnabled,
    notificationTime = notificationTime?.toString(),
    notificationTitle = notificationTitle,
    notificationBody = notificationBody,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorshipSongDto.toWorshipSong(): WorshipSong? { return WorshipSong(
    id = id ?: return null,
    scheduleId = scheduleId,
    programDate = programDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    programType = programType,
    title = title,
    artist = artist,
    youtubeUrl = youtubeUrl,
    youtubeVideoId = youtubeVideoId,
    thumbnailUrl = thumbnailUrl,
    momentLabel = momentLabel,
    notes = notes,
    orderIndex = orderIndex,
    isPublished = isPublished,
    isDailyRecommendation = isDailyRecommendation,
    recommendationDate = recommendationDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    recommendationMessage = recommendationMessage,
    notificationEnabled = notificationEnabled,
    notificationTime = notificationTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    notificationTitle = notificationTitle,
    notificationBody = notificationBody,
    createdAt = createdAt,
    updatedAt = updatedAt
) }
