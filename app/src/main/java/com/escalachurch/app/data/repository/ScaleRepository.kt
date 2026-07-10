package com.escalachurch.app.data.repository

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.ScaleDto
import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.data.remote.dto.toScaleItem
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.domain.model.ScaleItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow

/**
 * Official scales, backed by Supabase's `scales` table (see SupabaseTables). This is the single
 * source of truth read by every device; row-level security (see supabase/schema.sql) lets anyone
 * read but only an admin write.
 */
class ScaleRepository(private val client: SupabaseClient) {

    private val table get() = client.postgrest.from(SupabaseTables.SCALES)
    private val refreshTrigger = LocalRefreshTrigger()

    // Reads aren't restricted by RLS to one church (public read is open) - the app only ever
    // shows/writes its own configured church's data, so every read is scoped client-side. See
    // BuildConfig.CHURCH_ID (Fase 2 fix: this used to read every church's rows unfiltered).
    fun observeAll(): Flow<List<ScaleItem>> = client.observeTable(SupabaseTables.SCALES, refreshTrigger) {
        table.select { filter { eq("church_id", BuildConfig.CHURCH_ID) } }
            .decodeList<ScaleDto>()
            .mapNotNull { it.toScaleItem() }
    }

    suspend fun getById(id: String): ScaleItem? =
        table.select { filter { eq("id", id); eq("church_id", BuildConfig.CHURCH_ID) } }
            .decodeSingleOrNull<ScaleDto>()?.toScaleItem()

    /** Creates (blank id) or overwrites (existing id) a scale; returns the resulting row id. */
    suspend fun save(item: ScaleItem): String {
        val id = if (item.id.isBlank()) {
            table.insert(item.toDto()) { select(Columns.list("id")) }.decodeSingle<ScaleDto>().id!!
        } else {
            table.update(item.toDto()) { filter { eq("id", item.id) } }
            item.id
        }
        refreshTrigger.bump()
        return id
    }

    suspend fun delete(item: ScaleItem) {
        if (item.id.isBlank()) return
        table.delete { filter { eq("id", item.id) } }
        refreshTrigger.bump()
    }

    suspend fun deleteById(id: String) {
        if (id.isBlank()) return
        table.delete { filter { eq("id", id) } }
        refreshTrigger.bump()
    }
}
