package com.escalachurch.app.data.remote

import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Turns a Postgres table into a live [Flow]: fetches once immediately, then re-fetches the whole
 * table every time Realtime reports any insert/update/delete on it. Simpler and safer than trying
 * to patch individual rows into a cached list by hand, at the cost of an extra round trip per
 * change - fine at this app's scale (a handful of scales/announcements, not thousands).
 */
fun <T> io.github.jan.supabase.SupabaseClient.observeTable(
    table: String,
    fetch: suspend () -> List<T>
): Flow<List<T>> = callbackFlow {
    send(fetch())

    val realtimeChannel = channel("public:$table")
    val changeFlow = realtimeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
        this.table = table
    }

    val job = launch {
        changeFlow.collect {
            send(fetch())
        }
    }

    realtimeChannel.subscribe(blockUntilSubscribed = true)

    awaitClose { job.cancel() }
}
