package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.domain.model.UserClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class AnnouncementDto(
    val id: String? = null,
    // The backend is multi-tenant now (church_id is a required column) - defaulted here rather
    // than threaded through every call site, since this build only ever writes its own church's
    // row anyway (see BuildConfig.CHURCH_ID / local.properties).
    @SerialName("church_id") val churchId: String = BuildConfig.CHURCH_ID,
    val title: String = "",
    val description: String = "",
    @SerialName("media_type") val mediaType: String = MediaType.NONE.name,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("media_file_name") val mediaFileName: String? = null,
    @SerialName("image_aspect_ratio") val imageAspectRatio: String = "4:3",
    @SerialName("affected_classes") val affectedClasses: List<String> = emptyList(),
    @SerialName("related_event_date") val relatedEventDate: String? = null,
    @SerialName("source_type") val sourceType: String = SourceType.OFFICIAL.name,
    @SerialName("published_at") val publishedAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true
)

fun Announcement.toDto(churchId: String = BuildConfig.CHURCH_ID) = AnnouncementDto(
    id = id.ifBlank { null },
    churchId = churchId,
    title = title,
    description = description,
    mediaType = mediaType.name,
    mediaUrl = mediaUrl,
    mediaFileName = mediaFileName,
    imageAspectRatio = imageAspectRatio,
    affectedClasses = affectedClasses.map { it.name },
    relatedEventDate = relatedEventDate?.toString(),
    sourceType = sourceType.name,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isActive = isActive
)

fun AnnouncementDto.toAnnouncement(): Announcement? { return Announcement(
    id = id ?: return null,
    title = title,
    description = description,
    mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.NONE),
    mediaUrl = mediaUrl,
    mediaFileName = mediaFileName,
    imageAspectRatio = imageAspectRatio,
    affectedClasses = affectedClasses.mapNotNull { runCatching { UserClass.valueOf(it) }.getOrNull() }.toSet(),
    relatedEventDate = relatedEventDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    sourceType = runCatching { SourceType.valueOf(sourceType) }.getOrDefault(SourceType.OFFICIAL),
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isActive = isActive
) }
