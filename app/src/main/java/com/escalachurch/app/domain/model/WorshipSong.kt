package com.escalachurch.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** One item of "Música e Louvor" - backed by the existing `worship_songs` table (already shipped
 *  for the web site, see supabase/migrations/014-016) - no new table/columns for Android, per the
 *  task's own instruction. */
data class WorshipSong(
    val id: String = "",
    val scheduleId: String? = null,
    val programDate: LocalDate? = null,
    val programType: String? = null,
    val title: String,
    val artist: String = "",
    val youtubeUrl: String = "",
    val youtubeVideoId: String = "",
    val thumbnailUrl: String = "",
    val momentLabel: String = "",
    val notes: String = "",
    val orderIndex: Int = 0,
    val isPublished: Boolean = true,
    val isDailyRecommendation: Boolean = false,
    val recommendationDate: LocalDate? = null,
    val recommendationMessage: String? = null,
    val notificationEnabled: Boolean = false,
    val notificationTime: LocalTime? = null,
    val notificationTitle: String? = null,
    val notificationBody: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Whether this song is today's featured recommendation (Bloco A4: is_daily_recommendation +
     *  recommendation_date = hoje + is_published, all three). */
    fun isTodaysRecommendation(today: LocalDate): Boolean =
        isDailyRecommendation && isPublished && recommendationDate == today
}
