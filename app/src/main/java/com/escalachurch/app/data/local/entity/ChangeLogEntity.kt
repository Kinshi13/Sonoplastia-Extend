package com.escalachurch.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.escalachurch.app.domain.model.ChangeLogEntityType
import com.escalachurch.app.domain.model.ChangeLogEntry
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.domain.model.UserClass

@Entity(tableName = "change_log")
data class ChangeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entityType: ChangeLogEntityType,
    val entityId: String,
    /** Comma-separated [UserClass] names; see Converters. */
    val affectedClasses: String,
    val title: String,
    val message: String,
    val changedAt: Long,
    /** Comma-separated user ids who already saw this entry (single local user today: "local-user"). */
    val seenByUserIds: String,
    val sourceType: SourceType,
    val relatedDateIso: String?
)

fun ChangeLogEntity.toDomain() = ChangeLogEntry(
    id = id,
    entityType = entityType,
    entityId = entityId,
    affectedClasses = affectedClasses.split(',').filter { it.isNotBlank() }
        .mapNotNull { runCatching { UserClass.valueOf(it) }.getOrNull() }.toSet(),
    title = title,
    message = message,
    changedAt = changedAt,
    seenByUserIds = seenByUserIds.split(',').filter { it.isNotBlank() }.toSet(),
    sourceType = sourceType,
    relatedDateIso = relatedDateIso
)

fun ChangeLogEntry.toEntity() = ChangeLogEntity(
    id = id,
    entityType = entityType,
    entityId = entityId,
    affectedClasses = affectedClasses.joinToString(",") { it.name },
    title = title,
    message = message,
    changedAt = changedAt,
    seenByUserIds = seenByUserIds.joinToString(","),
    sourceType = sourceType,
    relatedDateIso = relatedDateIso
)
