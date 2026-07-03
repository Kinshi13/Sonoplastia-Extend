package com.escalachurch.app.data.repository

import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.DoxologyDto
import com.escalachurch.app.data.remote.dto.toDoxologyItem
import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.domain.model.DoxologyItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow

/** Official doxologies (order of service), backed by Supabase's `doxologies` table. */
class DoxologyRepository(private val client: SupabaseClient) {

    private val table get() = client.postgrest.from(SupabaseTables.DOXOLOGIES)

    fun observeAll(): Flow<List<DoxologyItem>> = client.observeTable(SupabaseTables.DOXOLOGIES) {
        table.select().decodeList<DoxologyDto>().mapNotNull { it.toDoxologyItem() }
    }

    suspend fun save(item: DoxologyItem): String {
        return if (item.id.isBlank()) {
            table.insert(item.toDto()) { select(Columns.list("id")) }.decodeSingle<DoxologyDto>().id!!
        } else {
            table.update(item.toDto()) { filter { eq("id", item.id) } }
            item.id
        }
    }

    suspend fun deleteById(id: String) {
        if (id.isBlank()) return
        table.delete { filter { eq("id", id) } }
    }
}
