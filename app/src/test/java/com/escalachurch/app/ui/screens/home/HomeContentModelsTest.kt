package com.escalachurch.app.ui.screens.home

import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.WorshipSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class HomeContentModelsTest {

    private fun scale(
        sound: String = "", conducting: String = "", preaching: String = "",
        reception: String = "", musical: String = "",
        isSpecialEvent: Boolean = false, isTemporary: Boolean = false
    ) = ScaleItem(
        date = LocalDate.of(2026, 9, 12), startTime = LocalTime.of(8, 45), title = "Culto Divino",
        soundPerson = sound, conductingPerson = conducting, preachingPerson = preaching,
        receptionPerson = reception, musicalMessagePerson = musical,
        isSpecialEvent = isSpecialEvent, isTemporary = isTemporary
    )

    @Test
    fun `hero shows up to 3 roles and counts the rest`() {
        val s = scale(sound = "Robson", conducting = "Ana", preaching = "Carlos", reception = "Maria", musical = "Pedro")
        val hero = heroSummaryFor(s)
        assertEquals(3, hero.visibleRoles.size)
        assertEquals(2, hero.hiddenRoleCount)
        assertEquals(listOf("Sonoplastia", "Regência", "Pregação"), hero.visibleRoles.map { it.roleLabel })
    }

    @Test
    fun `hero with 3 or fewer roles hides no count`() {
        val s = scale(sound = "Robson", conducting = "Ana")
        val hero = heroSummaryFor(s)
        assertEquals(2, hero.visibleRoles.size)
        assertEquals(0, hero.hiddenRoleCount)
    }

    @Test
    fun `hero never lists a blank role`() {
        val s = scale(sound = "Robson", conducting = "", preaching = "Carlos")
        val hero = heroSummaryFor(s)
        assertEquals(listOf("Sonoplastia", "Pregação"), hero.visibleRoles.map { it.roleLabel })
    }

    @Test
    fun `special label is null for a normal scale`() {
        assertNull(specialLabelFor(scale()))
    }

    @Test
    fun `special label reads special event as Programacao especial`() {
        assertEquals("Programação especial", specialLabelFor(scale(isSpecialEvent = true)))
    }

    @Test
    fun `special label reads temporary as extraordinaria and never shows the raw flag name`() {
        val label = specialLabelFor(scale(isTemporary = true))
        assertEquals("Programação extraordinária", label)
        assertTrue("must never leak the raw column name", label?.contains("is_temporary") != true)
    }

    @Test
    fun `when both flags are set, temporary (more specific) wins`() {
        assertEquals("Programação extraordinária", specialLabelFor(scale(isSpecialEvent = true, isTemporary = true)))
    }

    @Test
    fun `agora na igreja ranks pinned announcement before special schedule before daily music`() {
        val announcement = Announcement(id = "a1", title = "Aviso", isPinned = true)
        val special = scale(isSpecialEvent = true)
        val song = WorshipSong(id = "s1", title = "Música", isDailyRecommendation = true)

        val ranked = rankAgoraNaIgreja(announcement, special, song)

        assertEquals(3, ranked.size)
        assertTrue(ranked[0] is AgoraNaIgrejaItem.PinnedAnnouncement)
        assertTrue(ranked[1] is AgoraNaIgrejaItem.SpecialSchedule)
        assertTrue(ranked[2] is AgoraNaIgrejaItem.DailyMusic)
    }

    @Test
    fun `agora na igreja skips a normal (non-special) schedule entirely`() {
        val ranked = rankAgoraNaIgreja(null, scale(), null)
        assertTrue(ranked.isEmpty())
    }

    @Test
    fun `agora na igreja with nothing available is empty, not a crash`() {
        assertTrue(rankAgoraNaIgreja(null, null, null).isEmpty())
    }

    @Test
    fun `agora na igreja never exceeds 3 items`() {
        val announcement = Announcement(id = "a1", title = "Aviso", isPinned = true)
        val special = scale(isSpecialEvent = true, isTemporary = true) // still counts once
        val song = WorshipSong(id = "s1", title = "Música", isDailyRecommendation = true)
        val ranked = rankAgoraNaIgreja(announcement, special, song)
        assertTrue(ranked.size <= 3)
    }

    @Test
    fun `todays recommendation matches only the exact date and published flag`() {
        val today = LocalDate.of(2026, 9, 12)
        val match = WorshipSong(id = "1", title = "A", isDailyRecommendation = true, recommendationDate = today, isPublished = true)
        val wrongDate = match.copy(id = "2", recommendationDate = today.plusDays(1))
        val unpublished = match.copy(id = "3", isPublished = false)
        val notFlagged = match.copy(id = "4", isDailyRecommendation = false)

        assertEquals(match, todaysRecommendationFrom(listOf(wrongDate, unpublished, notFlagged, match), today))
        assertNull(todaysRecommendationFrom(listOf(wrongDate, unpublished, notFlagged), today))
    }
}
