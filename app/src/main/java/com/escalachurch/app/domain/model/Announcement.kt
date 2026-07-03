package com.escalachurch.app.domain.model

import java.time.LocalDate

enum class MediaType {
    NONE,
    IMAGE,
    VIDEO,
    /** PPT/PDF and other presentation/document files - shown as a download chip, not inline. */
    DOCUMENT
}

/**
 * A church-wide notice/event post shown in the Anúncios feed. Always [SourceType.OFFICIAL] in
 * this MVP - only an admin can publish one.
 *
 * [mediaUrl] is a Firebase Storage download URL (see AnnouncementRepository.uploadMedia) - files
 * are uploaded there right after being picked, so this always points at the same remote copy
 * whether it's opened from the phone or the web.
 */
data class Announcement(
    /** Firestore document id; empty string means "not saved yet". */
    val id: String = "",
    val title: String,
    val description: String = "",
    val mediaType: MediaType = MediaType.NONE,
    val mediaUrl: String? = null,
    /** Original file name, mainly used to label DOCUMENT attachments (e.g. "escala-domingo.pdf"). */
    val mediaFileName: String? = null,
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
