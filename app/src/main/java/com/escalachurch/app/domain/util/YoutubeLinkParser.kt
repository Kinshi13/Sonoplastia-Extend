package com.escalachurch.app.domain.util

/** Extracts a YouTube video id from any of the link shapes Música e Louvor accepts (Bloco A2).
 *  Pure string parsing - no network call, safe to unit test directly. */
object YoutubeLinkParser {

    private val patterns = listOf(
        Regex("(?:youtube\\.com|music\\.youtube\\.com)/watch\\?.*[?&]?v=([A-Za-z0-9_-]{6,})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{6,})"),
        Regex("youtube\\.com/shorts/([A-Za-z0-9_-]{6,})")
    )

    /** Returns the 11-ish char video id, or null if [url] doesn't match a known YouTube link shape. */
    fun extractVideoId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        for (pattern in patterns) {
            pattern.find(trimmed)?.let { return it.groupValues[1] }
        }
        return null
    }

    fun thumbnailUrl(videoId: String): String = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
}
