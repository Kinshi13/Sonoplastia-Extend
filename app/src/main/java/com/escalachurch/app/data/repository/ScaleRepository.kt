package com.escalachurch.app.data.repository

import com.escalachurch.app.data.remote.FirestoreCollections
import com.escalachurch.app.data.remote.observeAsFlow
import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.data.remote.dto.toScaleItem
import com.escalachurch.app.domain.model.ScaleItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Official scales, backed by Firestore's `scales` collection (see FirestoreCollections). This is
 * the single source of truth read by every device - the Firestore SDK keeps a disk cache and
 * serves it instantly offline, so the app still opens instantly without network.
 */
class ScaleRepository(private val firestore: FirebaseFirestore) {

    private val collection get() = firestore.collection(FirestoreCollections.SCALES)

    fun observeAll(): Flow<List<ScaleItem>> = collection.observeAsFlow { snapshot -> snapshot.toScaleItem() }

    suspend fun getById(id: String): ScaleItem? = collection.document(id).get().await().toScaleItem()

    /** Creates (blank id) or overwrites (existing id) a scale; returns the resulting document id. */
    suspend fun save(item: ScaleItem): String {
        return if (item.id.isBlank()) {
            collection.add(item.toDto()).await().id
        } else {
            collection.document(item.id).set(item.toDto()).await()
            item.id
        }
    }

    suspend fun delete(item: ScaleItem) {
        if (item.id.isBlank()) return
        collection.document(item.id).delete().await()
    }

    suspend fun deleteById(id: String) {
        if (id.isBlank()) return
        collection.document(id).delete().await()
    }
}
