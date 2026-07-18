package com.escalachurch.app.data.repository

import com.escalachurch.app.data.remote.dto.ChurchDto
import com.escalachurch.app.data.remote.dto.toChurch
import com.escalachurch.app.domain.model.Church
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fase 11.9 Parte 3 - data-layer contract for the church-code entry flow. Not wired into any
 * screen yet (ChurchEntryScreen itself is a later step) - this only establishes how a code
 * resolves to a church, ready for that screen to call.
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

    @Serializable
    private data class CodeParam(@SerialName("p_code") val code: String)
}
