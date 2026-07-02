package com.escalachurch.app.data.repository

import com.escalachurch.app.data.local.dao.AnnouncementDao
import com.escalachurch.app.data.local.entity.toDomain
import com.escalachurch.app.data.local.entity.toEntity
import com.escalachurch.app.domain.model.Announcement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * TODO(backend): once connected, active announcements + media should come from
 * Firebase Firestore/Storage or Supabase Database/Storage instead of Room; this
 * class is the single seam to swap - callers only ever see [Announcement].
 */
class AnnouncementRepository(private val dao: AnnouncementDao) {

    fun observeActive(): Flow<List<Announcement>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    suspend fun save(item: Announcement): Long = dao.upsert(item.toEntity())

    suspend fun delete(item: Announcement) = dao.delete(item.toEntity())

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
