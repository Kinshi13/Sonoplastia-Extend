package com.escalachurch.app.data.repository

import com.escalachurch.app.data.local.dao.ChangeLogDao
import com.escalachurch.app.data.local.entity.toDomain
import com.escalachurch.app.data.local.entity.toEntity
import com.escalachurch.app.domain.model.ChangeLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val LOCAL_USER_ID = "local-user"

/**
 * TODO(sync): once a backend exists, entries should arrive via push/remote sync instead of
 * being generated on save() calls made on this same device (see GeneralScaleRepository).
 */
class ChangeLogRepository(private val dao: ChangeLogDao) {

    fun observeAll(): Flow<List<ChangeLogEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * Unseen entries. When [onlyRelevant] is true, only entries whose affected classes overlap
     * [relevantClasses] match - if the user hasn't selected any class, nothing is "relevant" to
     * them, so nothing matches (this intentionally does NOT fall back to "show everything").
     * When [onlyRelevant] is false, every unseen official change matches, regardless of class.
     */
    fun observeUnseen(relevantClasses: Set<com.escalachurch.app.domain.model.UserClass>, onlyRelevant: Boolean): Flow<List<ChangeLogEntry>> =
        observeAll().map { entries ->
            entries.filter { entry ->
                val notSeen = LOCAL_USER_ID !in entry.seenByUserIds
                val matches = !onlyRelevant || entry.affectedClasses.any { it in relevantClasses }
                notSeen && matches
            }
        }

    suspend fun record(entry: ChangeLogEntry): Long = dao.insert(entry.toEntity())

    suspend fun markSeen(entry: ChangeLogEntry) {
        dao.update(entry.copy(seenByUserIds = entry.seenByUserIds + LOCAL_USER_ID).toEntity())
    }
}
