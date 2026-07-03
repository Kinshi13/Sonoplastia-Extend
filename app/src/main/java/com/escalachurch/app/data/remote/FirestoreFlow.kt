package com.escalachurch.app.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Turns a Firestore query (a whole collection, or a filtered/sorted [Query] over one - a
 * [com.google.firebase.firestore.CollectionReference] is itself a [Query]) into a live [Flow] of
 * mapped domain objects. Firestore's SDK already caches the last-known snapshot on disk and
 * serves it instantly while offline, so this keeps the app's existing "offline-first" feel
 * without any extra plumbing on our side.
 */
fun <T> Query.observeAsFlow(mapper: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            trySend(snapshot.documents.mapNotNull(mapper))
        }
    }
    awaitClose { registration.remove() }
}
