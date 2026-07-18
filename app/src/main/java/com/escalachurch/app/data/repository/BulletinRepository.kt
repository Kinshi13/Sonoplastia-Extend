package com.escalachurch.app.data.repository

import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.BulletinDto
import com.escalachurch.app.data.remote.dto.toBulletin
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.domain.model.Bulletin
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/** Boletins (PDF newsletters), backed by Supabase's `bulletins` table. Published from the admin
 *  website - the app only browses and opens them, there's no create/edit flow here. */
class BulletinRepository(private val client: SupabaseClient, private val activeChurchManager: ActiveChurchManager) {

    private val table get() = client.postgrest.from(SupabaseTables.BULLETINS)
    private val refreshTrigger = LocalRefreshTrigger()

    // See ScaleRepository.observeAll for why this filters by church_id client-side and where that
    // church_id comes from.
    fun observeActive(): Flow<List<Bulletin>> = activeChurchManager.activeChurchId.flatMapLatest { churchId ->
        client.observeTable(SupabaseTables.BULLETINS, refreshTrigger) {
            table.select { filter { eq("is_active", true); eq("church_id", churchId) } }
                .decodeList<BulletinDto>()
                .mapNotNull { it.toBulletin() }
                .sortedByDescending { it.publishedAt }
        }
    }
}
