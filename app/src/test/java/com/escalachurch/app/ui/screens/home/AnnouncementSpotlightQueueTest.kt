package com.escalachurch.app.ui.screens.home

import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.UserClass
import org.junit.Assert.assertEquals
import org.junit.Test

class AnnouncementSpotlightQueueTest {

    private fun announcement(
        id: String,
        updatedAt: Long = 1000L,
        isPinned: Boolean = false,
        affectedClasses: Set<UserClass> = emptySet()
    ) = Announcement(id = id, title = id, updatedAt = updatedAt, isPinned = isPinned, affectedClasses = affectedClasses)

    @Test
    fun newAnnouncement_notConfirmed_isEligible() {
        val queue = eligibleSpotlightQueue(
            announcements = listOf(announcement("a")),
            myClasses = emptySet(),
            confirmedUpdatedAt = emptyMap(),
            snoozedIds = emptySet()
        )
        assertEquals(listOf("a"), queue.map { it.id })
    }

    @Test
    fun alreadyConfirmed_sameUpdatedAt_isNotEligible() {
        val queue = eligibleSpotlightQueue(
            announcements = listOf(announcement("a", updatedAt = 1000L)),
            myClasses = emptySet(),
            confirmedUpdatedAt = mapOf("a" to 1000L),
            snoozedIds = emptySet()
        )
        assertEquals(emptyList<String>(), queue.map { it.id })
    }

    @Test
    fun confirmedButThenChanged_resurfaces() {
        val queue = eligibleSpotlightQueue(
            announcements = listOf(announcement("a", updatedAt = 2000L)),
            myClasses = emptySet(),
            confirmedUpdatedAt = mapOf("a" to 1000L),
            snoozedIds = emptySet()
        )
        assertEquals(listOf("a"), queue.map { it.id })
    }

    @Test
    fun snoozedThisSession_isExcluded() {
        val queue = eligibleSpotlightQueue(
            announcements = listOf(announcement("a")),
            myClasses = emptySet(),
            confirmedUpdatedAt = emptyMap(),
            snoozedIds = setOf("a")
        )
        assertEquals(emptyList<String>(), queue.map { it.id })
    }

    @Test
    fun directedAtOtherClass_isExcluded_directedAtEveryone_isIncluded() {
        val queue = eligibleSpotlightQueue(
            announcements = listOf(
                announcement("for-sonoplasta", affectedClasses = setOf(UserClass.SONOPLASTA)),
                announcement("for-everyone", affectedClasses = emptySet())
            ),
            myClasses = setOf(UserClass.REGENTE),
            confirmedUpdatedAt = emptyMap(),
            snoozedIds = emptySet()
        )
        assertEquals(listOf("for-everyone"), queue.map { it.id })
    }

    @Test
    fun pinnedFirst_thenMostRecentlyUpdated_cappedAtThree() {
        val queue = eligibleSpotlightQueue(
            announcements = listOf(
                announcement("old", updatedAt = 1000L),
                announcement("newest", updatedAt = 4000L),
                announcement("pinned-but-old", updatedAt = 500L, isPinned = true),
                announcement("mid", updatedAt = 2000L),
                announcement("also-recent", updatedAt = 3000L)
            ),
            myClasses = emptySet(),
            confirmedUpdatedAt = emptyMap(),
            snoozedIds = emptySet()
        )
        assertEquals(listOf("pinned-but-old", "newest", "also-recent"), queue.map { it.id })
    }
}
