package com.escalachurch.app.data.repository

import com.escalachurch.app.data.remote.FirestoreCollections
import com.escalachurch.app.data.remote.observeAsFlow
import com.escalachurch.app.data.remote.dto.toAnnouncement
import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.domain.model.Announcement
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Announcements + optional attached media, backed by Firestore's `announcements` collection.
 * Media is stored as an external link (Google Drive, YouTube, etc.) pasted in by the admin
 * rather than uploaded to Firebase Storage, since Storage requires the paid Blaze plan. Only an
 * admin (authenticated via Firebase Auth - see AdminSession) can write here; anyone can read.
 */
class AnnouncementRepository(
    private val firestore: FirebaseFirestore
) {

    private val collection get() = firestore.collection(FirestoreCollections.ANNOUNCEMENTS)

    // Sorted client-side (pinned first, newest first) instead of via Firestore orderBy() so this
    // never needs a composite index configured in the console - just whereEqualTo needs none.
    fun observeActive(): Flow<List<Announcement>> = collection
        .whereEqualTo("isActive", true)
        .observeAsFlow { snapshot -> snapshot.toAnnouncement() }
        .map { list -> list.sortedWith(compareByDescending<Announcement> { it.isPinned }.thenByDescending { it.publishedAt }) }

    suspend fun save(item: Announcement): String {
        return if (item.id.isBlank()) {
            collection.add(item.toDto()).await().id
        } else {
            collection.document(item.id).set(item.toDto()).await()
            item.id
        }
    }

    suspend fun delete(item: Announcement) {
        if (item.id.isBlank()) return
        collection.document(item.id).delete().await()
    }

    suspend fun deleteById(id: String) {
        if (id.isBlank()) return
        collection.document(id).delete().await()
    }
}
