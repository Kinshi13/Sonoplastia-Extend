package com.escalachurch.app.data.remote.dto

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.domain.model.Bulletin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Postgres-safe mirror of [Bulletin] for the `bulletins` table. Read-only from this app for
 *  now - Boletins are published from the admin website, the app only browses/opens them. */
@Serializable
data class BulletinDto(
    val id: String? = null,
    @SerialName("church_id") val churchId: String = BuildConfig.CHURCH_ID,
    val title: String = "",
    @SerialName("pdf_url") val pdfUrl: String = "",
    @SerialName("pdf_file_name") val pdfFileName: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("related_announcement_id") val relatedAnnouncementId: String? = null,
    @SerialName("published_at") val publishedAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("is_active") val isActive: Boolean = true
)

fun BulletinDto.toBulletin(): Bulletin? { return Bulletin(
    id = id ?: return null,
    title = title,
    pdfUrl = pdfUrl,
    pdfFileName = pdfFileName,
    coverUrl = coverUrl,
    relatedAnnouncementId = relatedAnnouncementId,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    isActive = isActive
) }
