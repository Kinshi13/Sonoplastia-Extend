package com.escalachurch.app.data.repository

import com.escalachurch.app.data.local.dao.CustomEventDao
import com.escalachurch.app.data.local.entity.toDomain
import com.escalachurch.app.data.local.entity.toEntity
import com.escalachurch.app.domain.model.CustomEvent
import com.escalachurch.app.domain.model.RepeatRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class CustomEventRepository(private val dao: CustomEventDao) {

    fun observeAll(): Flow<List<CustomEvent>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun save(item: CustomEvent): Long = dao.upsert(item.toEntity())

    suspend fun delete(item: CustomEvent) = dao.delete(item.toEntity())

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteGroup(groupId: String) = dao.deleteByGroupId(groupId)

    /**
     * Persists [base] plus one copy per extra date in [additionalDates] (manual multi-day
     * selection, e.g. Semana de Oração), and - if [base]'s repeatRule is WEEKLY - also
     * generates [weeklyOccurrences] further weekly copies. All generated rows share a groupId
     * so they can be identified/edited together later.
     */
    suspend fun saveWithDuplicates(
        base: CustomEvent,
        additionalDates: List<java.time.LocalDate> = emptyList(),
        weeklyOccurrences: Int = 0
    ): List<Long> {
        val groupId = if (additionalDates.isNotEmpty() || (base.repeatRule == RepeatRule.WEEKLY && weeklyOccurrences > 0)) {
            base.groupId ?: UUID.randomUUID().toString()
        } else {
            base.groupId
        }

        val occurrences = mutableListOf(base.copy(groupId = groupId))
        additionalDates.forEach { date ->
            occurrences += base.copy(id = 0L, date = date, groupId = groupId)
        }
        if (base.repeatRule == RepeatRule.WEEKLY && weeklyOccurrences > 0) {
            repeat(weeklyOccurrences) { week ->
                occurrences += base.copy(id = 0L, date = base.date.plusWeeks((week + 1).toLong()), groupId = groupId)
            }
        }
        return dao.insertAll(occurrences.map { it.toEntity() })
    }
}
