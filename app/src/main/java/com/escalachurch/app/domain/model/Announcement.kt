package com.escalachurch.app.domain.model

import java.time.LocalDate

enum class MediaType {
    NONE,
    IMAGE,
    VIDEO
}

/**
 * A church-wide notice/event post shown in the Anúncios feed. Always [SourceType.OFFICIAL] in
 * this MVP - only an admin can publish one.
 *
 * TODO(storage): [mediaUrl] is a local file/content URI for now. Once wired to a backend, swap
 * for a Firebase Storage / Supabase Storage download URL - the field itself doesn't need to
 * change, only how it's populated (AnnouncementRepository is the single seam to update).
 */
data class Announcement(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val mediaType: MediaType = MediaType.NONE,
    val mediaUrl: String? = null,
    /** Kept fixed at "4:3" per design guidance; modeled as a field so a future format can be added. */
    val imageAspectRatio: String = "4:3",
    val affectedClasses: Set<UserClass> = emptySet(),
    val relatedEventDate: LocalDate? = null,
    val createdBy: String = "",
    val sourceType: SourceType = SourceType.OFFICIAL,
    val publishedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isActive: Boolean = true
)
