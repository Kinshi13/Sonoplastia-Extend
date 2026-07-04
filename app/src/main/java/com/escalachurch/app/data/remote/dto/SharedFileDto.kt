package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.SharedFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedFileDto(
    val id: String? = null,
    // The backend is multi-tenant now (church_id is a required column) - defaulted here rather
    // than threaded through every call site, since this build only ever writes its own church's
    // row anyway (see BuildConfig.CHURCH_ID / local.properties).
    @SerialName("church_id") val churchId: String = BuildConfig.CHURCH_ID,
    @SerialName("file_name") val fileName: String = "",
    val url: String = "",
    @SerialName("media_type") val mediaType: String = MediaType.DOCUMENT.name,
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    @SerialName("uploaded_at") val uploadedAt: Long = 0L
)

fun SharedFile.toDto() = SharedFileDto(
    id = id.ifBlank { null },
    fileName = fileName,
    url = url,
    mediaType = mediaType.name,
    sizeBytes = sizeBytes,
    uploadedAt = uploadedAt
)

fun SharedFileDto.toSharedFile(): SharedFile? { return SharedFile(
    id = id ?: return null,
    fileName = fileName,
    url = url,
    mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.DOCUMENT),
    sizeBytes = sizeBytes,
    uploadedAt = uploadedAt
) }
