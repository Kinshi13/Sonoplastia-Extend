package com.escalachurch.app.domain.model

/** A PDF newsletter (Boletim) from a department/event, optionally linked to one announcement. */
data class Bulletin(
    val id: String = "",
    val title: String,
    val pdfUrl: String,
    val pdfFileName: String? = null,
    val coverUrl: String? = null,
    val relatedAnnouncementId: String? = null,
    val publishedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
