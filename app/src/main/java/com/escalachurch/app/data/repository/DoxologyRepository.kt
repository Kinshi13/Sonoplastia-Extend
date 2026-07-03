package com.escalachurch.app.data.repository

import com.escalachurch.app.data.remote.FirestoreCollections
import com.escalachurch.app.data.remote.observeAsFlow
import com.escalachurch.app.data.remote.dto.toDoxologyItem
import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.domain.model.DoxologyItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Official doxologies (order of service), backed by Firestore's `doxologies` collection. */
class DoxologyRepository(private val firestore: FirebaseFirestore) {

    private val collection get() = firestore.collection(FirestoreCollections.DOXOLOGIES)

    fun observeAll(): Flow<List<DoxologyItem>> = collection.observeAsFlow { snapshot -> snapshot.toDoxologyItem() }

    suspend fun save(item: DoxologyItem): String {
        return if (item.id.isBlank()) {
            collection.add(item.toDto()).await().id
        } else {
            collection.document(item.id).set(item.toDto()).await()
            item.id
        }
    }

    suspend fun deleteById(id: String) {
        if (id.isBlank()) return
        collection.document(id).delete().await()
    }
}
