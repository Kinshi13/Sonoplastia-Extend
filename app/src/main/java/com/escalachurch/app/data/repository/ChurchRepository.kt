package com.escalachurch.app.data.repository

import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.ChurchDto
import com.escalachurch.app.data.remote.dto.toChurch
import com.escalachurch.app.domain.model.Church
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fase 11.9A - data-layer for the church-code entry flow: resolving a typed code to a church
 * (ChurchEntryScreen) and resolving an admin's own church by id after login (AdminSession).
 */
class ChurchRepository(private val client: SupabaseClient) {

    /** Null means "no church with this code" - never throws for a not-found code, only for an
     *  actual network/server failure, so callers can tell "typo" apart from "offline". */
    suspend fun findByCode(code: String): Church? {
        return client.postgrest.rpc("get_church_by_code", CodeParam(code))
            .decodeList<ChurchDto>()
            .firstOrNull()
            ?.toChurch()
    }

    /** `churches` is fully public-read (see schema.sql) - a direct table lookup by id is exactly
     *  as safe as the code RPC, just keyed differently (used when the id is already known, e.g.
     *  from profiles.church_id or a persisted RecentChurch, not typed by the user). */
    suspend fun findById(id: String): Church? = runCatching {
        client.postgrest.from(SupabaseTables.CHURCHES)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<ChurchDto>()
            ?.toChurch()
    }.getOrNull()

    @Serializable
    private data class CodeParam(@SerialName("p_code") val code: String)
}
