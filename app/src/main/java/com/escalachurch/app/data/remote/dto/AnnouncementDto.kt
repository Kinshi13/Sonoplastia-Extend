package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.domain.model.UserClass
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate

data class AnnouncementDto(
    val title: String = "",
    val description: String = "",
    val mediaType: String = MediaType.NONE.name,
    val mediaUrl: String? = null,
    val mediaFileName: String? = null,
    val imageAspectRatio: String = "4:3",
    val affectedClasses: List<String> = emptyList(),
    val relatedEventDate: String? = null,
    val createdBy: String = "",
    val sourceType: String = SourceType.OFFICIAL.name,
    val publishedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isPinned: Boolean = false,
    val isActive: Boolean = true
)

fun Announcement.toDto() = AnnouncementDto(
    title = title,
    description = description,
    mediaType = mediaType.name,
    mediaUrl = mediaUrl,
    mediaFileName = mediaFileName,
    imageAspectRatio = imageAspectRatio,
    affectedClasses = affectedClasses.map { it.name },
    relatedEventDate = relatedEventDate?.toString(),
    createdBy = createdBy,
    sourceType = sourceType.name,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isActive = isActive
)

fun DocumentSnapshot.toAnnouncement(): Announcement? {
    val dto = toObject(AnnouncementDto::class.java) ?: return null
    return Announcement(
        id = id,
        title = dto.title,
        description = dto.description,
        mediaType = runCatching { MediaType.valueOf(dto.mediaType) }.getOrDefault(MediaType.NONE),
        mediaUrl = dto.mediaUrl,
        mediaFileName = dto.mediaFileName,
        imageAspectRatio = dto.imageAspectRatio,
        affectedClasses = dto.affectedClasses.mapNotNull { runCatching { UserClass.valueOf(it) }.getOrNull() }.toSet(),
        relatedEventDate = dto.relatedEventDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        createdBy = dto.createdBy,
        sourceType = runCatching { SourceType.valueOf(dto.sourceType) }.getOrDefault(SourceType.OFFICIAL),
        publishedAt = dto.publishedAt,
        updatedAt = dto.updatedAt,
        isPinned = dto.isPinned,
        isActive = dto.isActive
    )
}
