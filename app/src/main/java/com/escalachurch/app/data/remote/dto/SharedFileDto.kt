package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.SharedFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedFileDto(
    val id: String? = null,
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
