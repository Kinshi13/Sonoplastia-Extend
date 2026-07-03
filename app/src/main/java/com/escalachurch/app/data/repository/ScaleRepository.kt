package com.escalachurch.app.data.repository

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

    fun observeAll(): Flow<List<ScaleItem>> = client.observeTable(SupabaseTables.SCALES, refreshTrigger) {
        table.select().decodeList<ScaleDto>().mapNotNull { it.toScaleItem() }
    }

    suspend fun getById(id: String): ScaleItem? =
        table.select { filter { eq("id", id) } }.decodeSingleOrNull<ScaleDto>()?.toScaleItem()

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
