package com.escalachurch.app.data.repository

import com.escalachurch.app.data.local.dao.DoxologyDao
import com.escalachurch.app.data.local.entity.toDomain
import com.escalachurch.app.data.local.entity.toEntity
import com.escalachurch.app.domain.model.DoxologyItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DoxologyRepository(private val dao: DoxologyDao) {

    fun observeAll(): Flow<List<DoxologyItem>> =
        combine(dao.observeAll(), dao.observeAllSteps()) { doxologies, steps ->
            doxologies.map { doxology ->
                val ownSteps = steps.filter { it.doxologyId == doxology.id }.map { it.toDomain() }
                doxology.toDomain(ownSteps)
            }
        }

    suspend fun save(item: DoxologyItem): Long {
        val id = dao.upsert(item.toEntity())
        dao.deleteStepsForDoxology(id)
        if (item.programOrder.isNotEmpty()) {
            dao.insertSteps(item.programOrder.map { it.toEntity(id) })
        }
        return id
    }

    suspend fun deleteById(id: Long) {
        dao.deleteStepsForDoxology(id)
        dao.deleteById(id)
    }
}
