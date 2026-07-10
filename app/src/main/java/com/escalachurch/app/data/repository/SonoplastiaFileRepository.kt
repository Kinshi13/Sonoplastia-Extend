package com.escalachurch.app.data.repository

import android.content.Context
import android.net.Uri
import com.escalachurch.app.BuildConfig
import com.escalachurch.app.data.remote.LocalRefreshTrigger
import com.escalachurch.app.data.remote.SupabaseTables
import com.escalachurch.app.data.remote.dto.SharedFileDto
import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.data.remote.dto.toSharedFile
import com.escalachurch.app.data.remote.observeTable
import com.escalachurch.app.domain.model.SharedFile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow

/**
 * Files shared remotely between phone and PC via the Sonoplastia screen (PPT/PDF/photos/videos
 * used in a service). Backed by Supabase Storage (the actual bytes, under `sonoplastia/`) plus
 * the `shared_files` table (metadata, so the list can be shown/sorted without listing Storage).
 */
class SonoplastiaFileRepository(private val client: SupabaseClient) {

    private val table get() = client.postgrest.from(SupabaseTables.SHARED_FILES)
    private val refreshTrigger = LocalRefreshTrigger()

    // See ScaleRepository.observeAll for why this filters by church_id client-side.
    fun observeAll(): Flow<List<SharedFile>> = client.observeTable(SupabaseTables.SHARED_FILES, refreshTrigger) {
        table.select { filter { eq("church_id", BuildConfig.CHURCH_ID) } }
            .decodeList<SharedFileDto>()
            .mapNotNull { it.toSharedFile() }
            .sortedByDescending { it.uploadedAt }
    }

    suspend fun upload(context: Context, uri: Uri): SharedFile {
        val uploaded = client.uploadToChurchFiles(context, uri, "sonoplastia")
        val fileName = uploaded.fileName ?: "arquivo"
        val record = SharedFile(
            fileName = fileName,
            url = uploaded.url,
            mediaType = uploaded.type,
            uploadedAt = System.currentTimeMillis()
        )
        val saved = table.insert(record.toDto()) { select() }.decodeSingle<SharedFileDto>()
        refreshTrigger.bump()
        return saved.toSharedFile() ?: record
    }

    suspend fun delete(file: SharedFile) {
        if (file.id.isBlank()) return
        table.delete { filter { eq("id", file.id) } }
        refreshTrigger.bump()
    }
}
