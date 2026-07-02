package com.escalachurch.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.domain.model.UserClass
import java.time.LocalDate

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val mediaType: MediaType,
    val mediaUrl: String?,
    val imageAspectRatio: String,
    /** Comma-separated [UserClass] names; see Converters. */
    val affectedClasses: String,
    val relatedEventDate: LocalDate?,
    val createdBy: String,
    val sourceType: SourceType,
    val publishedAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val isActive: Boolean
)

fun AnnouncementEntity.toDomain() = Announcement(
    id = id,
    title = title,
    description = description,
    mediaType = mediaType,
    mediaUrl = mediaUrl,
    imageAspectRatio = imageAspectRatio,
    affectedClasses = affectedClasses.split(',').filter { it.isNotBlank() }
        .mapNotNull { runCatching { UserClass.valueOf(it) }.getOrNull() }.toSet(),
    relatedEventDate = relatedEventDate,
    createdBy = createdBy,
    sourceType = sourceType,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isActive = isActive
)

fun Announcement.toEntity() = AnnouncementEntity(
    id = id,
    title = title,
    description = description,
    mediaType = mediaType,
    mediaUrl = mediaUrl,
    imageAspectRatio = imageAspectRatio,
    affectedClasses = affectedClasses.joinToString(",") { it.name },
    relatedEventDate = relatedEventDate,
    createdBy = createdBy,
    sourceType = sourceType,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    isActive = isActive
)
