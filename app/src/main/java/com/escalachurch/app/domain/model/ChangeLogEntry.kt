package com.escalachurch.app.domain.model

/** Kind of entity a [ChangeLogEntry] refers to. */
enum class ChangeLogEntityType {
    SCALE,
    DOXOLOGY,
    ANNOUNCEMENT,
    EVENT
}

/**
 * Records a relevant change to official data (a scale, doxology, announcement or event) so the
 * app can surface it to affected members: a local notification and an in-app pop-up on next open.
 *
 * TODO(sync): once a backend exists, entries should be written server-side (Cloud
 * Function/Supabase trigger) when an admin saves official data, and delivered to devices via
 * Firebase Cloud Messaging / push, instead of being generated on-device as they are today.
 */
data class ChangeLogEntry(
    val id: Long = 0L,
    val entityType: ChangeLogEntityType,
    /** Firestore document id of the scale/doxology/announcement this change refers to. */
    val entityId: String,
    val affectedClasses: Set<UserClass>,
    val title: String,
    val message: String,
    val changedAt: Long = System.currentTimeMillis(),
    val seenByUserIds: Set<String> = emptySet(),
    val sourceType: SourceType = SourceType.OFFICIAL,
    /** Date (ISO string, e.g. "2026-09-14") the change relates to, so a pop-up can deep-link to it. */
    val relatedDateIso: String? = null
)
