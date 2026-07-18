package com.escalachurch.app.data.repository

import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.ProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

/** Read-only lookup of the authenticated user's own `profiles` row - used only right after Admin
 *  login to resolve which church that account belongs to (see AdminSession.signIn). */
class ProfileRepository(private val client: SupabaseClient) {

    suspend fun fetchChurchId(userId: String): String? = runCatching {
        client.postgrest.from(SupabaseTables.PROFILES)
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<ProfileDto>()
            ?.churchId
    }.getOrNull()
}
