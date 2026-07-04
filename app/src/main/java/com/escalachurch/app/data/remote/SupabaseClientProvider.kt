package com.escalachurch.app.data.remote

import com.escalachurch.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

/** One shared Supabase client for the whole app - URL/key come from local.properties at build time. */
object SupabaseClientProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // Default Kotlin serialization rejects unknown JSON keys - any column added to a
            // table server-side (e.g. the multi-tenant church_id migration) would otherwise
            // crash every read on this app version. Ignoring unknown keys keeps old app
            // versions working across additive schema changes.
            defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
            install(Postgrest)
            install(Auth)
            install(Storage)
            install(Realtime)
        }
    }
}
