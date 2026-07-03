package com.escalachurch.app.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.escalachurch.app.data.remote.CHURCH_FILES_BUCKET
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.AnnouncementDto
import com.escalachurch.app.data.remote.dto.toAnnouncement
import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** What a picked file was uploaded as, ready to attach to an [Announcement]. */
data class UploadedMedia(val type: MediaType, val url: String, val fileName: String?)

/** Announcements + optional media, backed by Supabase's `announcements` table + Storage bucket. */
class AnnouncementRepository(private val client: SupabaseClient) {

    private val table get() = client.postgrest.from(SupabaseTables.ANNOUNCEMENTS)
    private val refreshTrigger = LocalRefreshTrigger()

    fun observeActive(): Flow<List<Announcement>> = client.observeTable(SupabaseTables.ANNOUNCEMENTS, refreshTrigger) {
        table.select { filter { eq("is_active", true) } }
            .decodeList<AnnouncementDto>()
            .mapNotNull { it.toAnnouncement() }
            .sortedWith(compareByDescending<Announcement> { it.isPinned }.thenByDescending { it.publishedAt })
    }

    suspend fun save(item: Announcement): String {
        val id = if (item.id.isBlank()) {
            table.insert(item.toDto()) { select(Columns.list("id")) }.decodeSingle<AnnouncementDto>().id!!
        } else {
            table.update(item.toDto()) { filter { eq("id", item.id) } }
            item.id
        }
        refreshTrigger.bump()
        return id
    }

    suspend fun delete(item: Announcement) {
        if (item.id.isBlank()) return
        table.delete { filter { eq("id", item.id) } }
        refreshTrigger.bump()
    }

    suspend fun deleteById(id: String) {
        if (id.isBlank()) return
        table.delete { filter { eq("id", id) } }
        refreshTrigger.bump()
    }

    /** Uploads a locally-picked file (image, video, PPT/PDF) to Supabase Storage and returns its public URL. */
    suspend fun uploadMedia(context: Context, uri: Uri): UploadedMedia =
        client.uploadToChurchFiles(context, uri, "announcements")
}

/** Shared by [AnnouncementRepository] and the Sonoplastia file-sharing screen. */
suspend fun SupabaseClient.uploadToChurchFiles(context: Context, uri: Uri, folder: String): UploadedMedia {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri).orEmpty()
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
    val fileName = queryDisplayName(context, uri)

    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    val path = "$folder/${UUID.randomUUID()}.$extension"
    storage.from(CHURCH_FILES_BUCKET).upload(path, bytes)
    val url = storage.from(CHURCH_FILES_BUCKET).publicUrl(path)

    val type = when {
        mimeType.startsWith("image/") -> MediaType.IMAGE
        mimeType.startsWith("video/") -> MediaType.VIDEO
        else -> MediaType.DOCUMENT
    }
    return UploadedMedia(type = type, url = url, fileName = fileName)
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }.getOrNull()
}
