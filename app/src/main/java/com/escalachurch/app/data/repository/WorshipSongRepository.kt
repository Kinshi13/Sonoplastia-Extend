package com.escalachurch.app.data.repository

import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.WorshipSongDto
import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.data.remote.dto.toWorshipSong
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.domain.model.WorshipSong
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.util.UUID

/** "Música e Louvor" - reuses `public.worship_songs`, already shipped for the web site (see
 *  supabase/migrations/014-016). No new table/column added here - same RLS, same RPC-free model
 *  as every other public+admin table this app already reads. */
class WorshipSongRepository(private val client: SupabaseClient, private val activeChurchManager: ActiveChurchManager) {

    private val table get() = client.postgrest.from(SupabaseTables.WORSHIP_SONGS)
    private val refreshTrigger = LocalRefreshTrigger()

    /** Public feed - published only, ordered the same way the admin arranges them. */
    fun observePublished(): Flow<List<WorshipSong>> = activeChurchManager.activeChurchId.flatMapLatest { churchId ->
        client.observeTable(SupabaseTables.WORSHIP_SONGS, refreshTrigger) {
            table.select { filter { eq("church_id", churchId); eq("is_published", true) } }
                .decodeList<WorshipSongDto>()
                .mapNotNull { it.toWorshipSong() }
                .sortedWith(compareBy({ it.programDate?.toString() ?: "" }, { it.orderIndex }))
        }
    }

    /** Admin feed - everything, published or not, same "edit what you see" pattern as
     *  AnnouncementRepository.observeAllForAdmin. */
    fun observeAllForAdmin(): Flow<List<WorshipSong>> = activeChurchManager.activeChurchId.flatMapLatest { churchId ->
        client.observeTable(SupabaseTables.WORSHIP_SONGS, refreshTrigger) {
            table.select { filter { eq("church_id", churchId) } }
                .decodeList<WorshipSongDto>()
                .mapNotNull { it.toWorshipSong() }
                .sortedWith(compareBy({ it.programDate?.toString() ?: "" }, { it.orderIndex }))
        }
    }

    suspend fun save(item: WorshipSong): String {
        val churchId = activeChurchManager.activeChurchId.first()
        val id = if (item.id.isBlank()) {
            // Fase 11.11 (correção preventiva) - same fix as AnnouncementRepository.save(): id
            // generated client-side instead of relying on `select(Columns.list("id"))` reading the
            // freshly-inserted row back. `worship_songs` only has a public SELECT policy scoped to
            // `is_published` - saving a draft (Publicado desmarcado) would insert successfully and
            // then fail to read it back, since RETURNING is also filtered by the SELECT policy.
            val newId = UUID.randomUUID().toString()
            table.insert(item.toDto(churchId).copy(id = newId))
            newId
        } else {
            table.update(item.toDto(churchId)) { filter { eq("id", item.id) } }
            item.id
        }
        refreshTrigger.bump()
        return id
    }

    /** Publicar/despublicar (Bloco A3) - full-row update via [save], same pattern every other
     *  repository here uses for a toggle (no separate partial-update SDK call to keep verified). */
    suspend fun setPublished(item: WorshipSong, isPublished: Boolean): String =
        save(item.copy(isPublished = isPublished, updatedAt = System.currentTimeMillis()))

    suspend fun delete(item: WorshipSong) {
        if (item.id.isBlank()) return
        table.delete { filter { eq("id", item.id) } }
        refreshTrigger.bump()
    }
}
