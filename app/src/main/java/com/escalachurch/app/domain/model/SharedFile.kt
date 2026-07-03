package com.escalachurch.app.domain.model

/** A file shared remotely between phone and PC through the Sonoplastia screen. */
data class SharedFile(
    val id: String = "",
    val fileName: String,
    val url: String,
    val mediaType: MediaType = MediaType.DOCUMENT,
    val sizeBytes: Long = 0L,
    val uploadedAt: Long = System.currentTimeMillis()
)
