package com.escalachurch.app.data.remote

import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Bumped by a repository right after its own successful save/delete/upload, so the writer's own
 * view refreshes immediately without waiting on (or depending on) the Realtime websocket, which
 * can silently fail to confirm a subscription in some network conditions. Other devices still get
 * the live update via Realtime when that's working; this is just a same-device safety net.
 */
class LocalRefreshTrigger {
    private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun bump() { flow.tryEmit(Unit) }
    fun asFlow(): Flow<Unit> = flow
}

/**
 * Turns a Postgres table into a live [Flow]: fetches once immediately, then re-fetches the whole
 * table every time Realtime reports a change, or [localTrigger] is bumped. Simpler and safer than
 * trying to patch individual rows into a cached list by hand, at the cost of an extra round trip
 * per change - fine at this app's scale (a handful of scales/announcements, not thousands).
 *
 * Realtime subscription failures are swallowed rather than propagated: if the websocket can't
 * confirm a subscription, the table is still readable via the initial fetch and [localTrigger] -
 * it just won't live-update from other devices until Realtime recovers.
 */
fun <T> io.github.jan.supabase.SupabaseClient.observeTable(
    table: String,
    localTrigger: LocalRefreshTrigger,
    fetch: suspend () -> List<T>
): Flow<List<T>> = callbackFlow {
    send(fetch())

    val job = launch {
        runCatching {
            val realtimeChannel = channel("public:$table")
            val changeFlow = realtimeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                this.table = table
            }
            realtimeChannel.subscribe(blockUntilSubscribed = true)
            merge(changeFlow.map { }, localTrigger.asFlow()).collect { send(fetch()) }
        }.onFailure {
            // Realtime unavailable - fall back to local-trigger-only refresh.
            localTrigger.asFlow().collect { send(fetch()) }
        }
    }

    awaitClose { job.cancel() }
}
