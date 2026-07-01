package com.escalachurch.app.data.repository

import com.escalachurch.app.data.local.dao.ScaleDao
import com.escalachurch.app.data.local.entity.toDomain
import com.escalachurch.app.data.local.entity.toEntity
import com.escalachurch.app.domain.model.ScaleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScaleRepository(private val dao: ScaleDao) {

    fun observeAll(): Flow<List<ScaleItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun save(item: ScaleItem): Long = dao.upsert(item.toEntity())

    suspend fun update(item: ScaleItem) = dao.update(item.toEntity())

    suspend fun delete(item: ScaleItem) = dao.delete(item.toEntity())

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
