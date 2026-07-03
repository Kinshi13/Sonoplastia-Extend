package com.escalachurch.app.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.escalachurch.app.data.remote.FirestoreCollections
import com.escalachurch.app.data.remote.observeAsFlow
import com.escalachurch.app.data.remote.dto.toAnnouncement
import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** What a picked file was uploaded as, ready to attach to an [Announcement]. */
data class UploadedMedia(val type: MediaType, val url: String, val fileName: String?)

/** Thrown by [AnnouncementRepository.uploadMedia] when the picked file is too large to upload. */
class MediaTooLargeException(val maxSizeMb: Int) : Exception("File exceeds ${maxSizeMb}MB limit")

private const val MAX_UPLOAD_BYTES = 20L * 1024 * 1024

/**
 * Announcements + their attached media (image/video/PPT/PDF), backed by Firestore's
 * `announcements` collection and Firebase Storage for the files themselves. Only an admin
 * (authenticated via Firebase Auth - see AdminSession) can write here; anyone can read.
 */
class AnnouncementRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
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

    /**
     * Uploads a locally-picked file (image, video, PPT/PDF) to Firebase Storage and returns its
     * public download URL, ready to store as [Announcement.mediaUrl]. Presentation files (PPT,
     * PDF) are classified as [MediaType.DOCUMENT] so the feed shows a download chip instead of
     * trying to preview them inline.
     *
     * Rejects files over 20MB client-side (see [MediaTooLargeException]) to protect the free
     * Storage quota from an accidental large video upload; the same limit is also enforced
     * server-side by the Storage security rules, since a client-side check alone isn't trustworthy.
     */
    suspend fun uploadMedia(context: Context, uri: Uri): UploadedMedia {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri).orEmpty()
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val fileName = queryDisplayName(context, uri)

        val size = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (size > MAX_UPLOAD_BYTES) {
            throw MediaTooLargeException(maxSizeMb = (MAX_UPLOAD_BYTES / (1024 * 1024)).toInt())
        }

        val type = when {
            mimeType.startsWith("image/") -> MediaType.IMAGE
            mimeType.startsWith("video/") -> MediaType.VIDEO
            else -> MediaType.DOCUMENT
        }

        val storageRef = storage.reference.child("announcements/${UUID.randomUUID()}.$extension")
        storageRef.putFile(uri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        return UploadedMedia(type = type, url = downloadUrl, fileName = fileName)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        }.getOrNull()
    }
}
