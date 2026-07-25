package com.escalachurch.app.ui.screens.announcements

import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.domain.model.UserClass
import java.time.LocalDate

/**
 * Usabilidade (edição de anúncios) - a plain snapshot of every field [AnnouncementEditScreen]'s
 * form edits, used both as the "original" baseline (captured once, from [existing]) and the
 * "current" value (recomputed on every recomposition) so "alterações não salvas" can be answered
 * by a single, directly-testable equality check instead of comparing scattered `remember` vars by
 * hand at the call site.
 */
data class AnnouncementDraft(
    val title: String,
    val description: String,
    val mediaType: MediaType,
    val mediaUrl: String?,
    val mediaFileName: String?,
    val affectedClasses: Set<UserClass>,
    val relatedEventDate: LocalDate?,
    val isPinned: Boolean,
    val isActive: Boolean
)

fun announcementDraftFrom(existing: Announcement?): AnnouncementDraft = AnnouncementDraft(
    title = existing?.title ?: "",
    description = existing?.description ?: "",
    mediaType = existing?.mediaType ?: MediaType.NONE,
    mediaUrl = existing?.mediaUrl,
    mediaFileName = existing?.mediaFileName,
    affectedClasses = existing?.affectedClasses ?: emptySet(),
    relatedEventDate = existing?.relatedEventDate,
    isPinned = existing?.isPinned ?: false,
    isActive = existing?.isActive ?: true
)

/** True when [current] differs from [original] in any field the form actually lets the Admin
 *  touch - the trigger for the "Descartar alterações?" prompt on back/cancel. Pure - see
 *  AnnouncementEditLogicTest. */
fun hasUnsavedAnnouncementChanges(original: AnnouncementDraft, current: AnnouncementDraft): Boolean =
    original != current

/** A title of only whitespace is the same as no title at all - trimmed before the blank check so
 *  "   " is correctly rejected, matching "não aceitar somente espaços". Pure - see
 *  AnnouncementEditLogicTest. */
fun isValidAnnouncementTitle(title: String): Boolean = title.trim().isNotEmpty()

fun buildAnnouncementFromDraft(existing: Announcement?, draft: AnnouncementDraft): Announcement = Announcement(
    id = existing?.id ?: "",
    title = draft.title,
    description = draft.description,
    mediaType = draft.mediaType,
    mediaUrl = draft.mediaUrl,
    mediaFileName = draft.mediaFileName,
    affectedClasses = draft.affectedClasses,
    relatedEventDate = draft.relatedEventDate,
    sourceType = SourceType.OFFICIAL,
    isPinned = draft.isPinned,
    isActive = draft.isActive,
    publishedAt = existing?.publishedAt ?: System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)
