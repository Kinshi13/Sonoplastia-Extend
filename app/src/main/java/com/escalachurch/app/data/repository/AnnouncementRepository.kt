package com.escalachurch.app.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.escalachurch.app.church.ActiveChurchManager
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
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.util.UUID

/** What a picked file was uploaded as, ready to attach to an [Announcement]. */
data class UploadedMedia(val type: MediaType, val url: String, val fileName: String?)

/** Announcements + optional media, backed by Supabase's `announcements` table + Storage bucket. */
class AnnouncementRepository(private val client: SupabaseClient, private val activeChurchManager: ActiveChurchManager) {

    private val table get() = client.postgrest.from(SupabaseTables.ANNOUNCEMENTS)
    private val refreshTrigger = LocalRefreshTrigger()

    // See ScaleRepository.observeAll for why this filters by church_id client-side and where that
    // church_id comes from.
    fun observeActive(): Flow<List<Announcement>> = activeChurchManager.activeChurchId.flatMapLatest { churchId ->
        client.observeTable(SupabaseTables.ANNOUNCEMENTS, refreshTrigger) {
            table.select { filter { eq("is_active", true); eq("church_id", churchId) } }
                .decodeList<AnnouncementDto>()
                .mapNotNull { it.toAnnouncement() }
                .sortedWith(compareByDescending<Announcement> { it.isPinned }.thenByDescending { it.publishedAt })
        }
    }

    /** Usabilidade (edição de anúncios) - same query as [observeActive] but without the
     *  `is_active` filter, so an Admin who unpublishes an announcement can still find it again to
     *  edit or republish it, instead of it vanishing from their own feed. Never used for the
     *  public/member feed - [AnnouncementViewModel] only switches to this when `isAdmin` is true. */
    fun observeAllForAdmin(): Flow<List<Announcement>> = activeChurchManager.activeChurchId.flatMapLatest { churchId ->
        client.observeTable(SupabaseTables.ANNOUNCEMENTS, refreshTrigger) {
            table.select { filter { eq("church_id", churchId) } }
                .decodeList<AnnouncementDto>()
                .mapNotNull { it.toAnnouncement() }
                .sortedWith(compareByDescending<Announcement> { it.isPinned }.thenByDescending { it.publishedAt })
        }
    }

    suspend fun save(item: Announcement): String {
        val churchId = activeChurchManager.activeChurchId.first()
        val id = if (item.id.isBlank()) {
            // Homologação (correção de criação de anúncios): the id is generated here, client-side,
            // instead of relying on `select(Columns.list("id"))` reading the row back right after
            // insert. `announcements` only has a public SELECT policy scoped to `is_active` - saving
            // a draft (Publicado desmarcado) inserts a row RLS then refuses to hand back to the very
            // same INSERT, since Postgres also applies the SELECT policy to a RETURNING clause. That
            // turned "salvar rascunho" into a guaranteed failure with no rows returned, regardless of
            // church_id/is_admin being perfectly correct. A client-generated id sidesteps this
            // without touching RLS - `id` still gets Postgres's own `gen_random_uuid()` default type,
            // just supplied instead of relied upon.
            val newId = UUID.randomUUID().toString()
            table.insert(item.toDto(churchId).copy(id = newId))
            newId
        } else {
            table.update(item.toDto(churchId)) { filter { eq("id", item.id) } }
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
