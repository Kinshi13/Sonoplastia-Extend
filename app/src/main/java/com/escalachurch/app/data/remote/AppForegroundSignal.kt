package com.escalachurch.app.data.remote

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * App-wide "we just came back to the foreground" signal, registered on ProcessLifecycleOwner in
 * EscalaChurchApp.onCreate. Supabase Realtime's websocket can silently die while the app is
 * backgrounded (Doze, the OS tearing down idle sockets, a flaky connection) with no error ever
 * surfaced - observeTable() listens to this alongside its own local/realtime triggers so every
 * currently-visible screen force-refreshes the moment the app resumes, instead of only picking up
 * the change on the next navigation (or never, if the user never leaves that screen).
 */
object AppForegroundSignal : DefaultLifecycleObserver {
    private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = flow

    override fun onStart(owner: LifecycleOwner) {
        flow.tryEmit(Unit)
    }
}
