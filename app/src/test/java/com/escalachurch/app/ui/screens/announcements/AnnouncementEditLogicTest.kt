package com.escalachurch.app.ui.screens.announcements

import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.UserClass
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementEditLogicTest {

    private val existing = Announcement(
        id = "abc",
        title = "Culto Jovem",
        description = "Venha participar",
        mediaType = MediaType.IMAGE,
        mediaUrl = "https://example.com/image.jpg",
        mediaFileName = "image.jpg",
        affectedClasses = setOf(UserClass.SONOPLASTA),
        relatedEventDate = LocalDate.of(2026, 7, 25),
        isPinned = true,
        isActive = true
    )

    // --- validação de campos ---

    @Test
    fun blankTitleIsInvalid() {
        assertFalse(isValidAnnouncementTitle(""))
    }

    @Test
    fun whitespaceOnlyTitleIsInvalid() {
        assertFalse(isValidAnnouncementTitle("   "))
    }

    @Test
    fun nonBlankTitleIsValid() {
        assertTrue(isValidAnnouncementTitle("Culto Jovem"))
    }

    // --- estado de alterações não salvas ---

    @Test
    fun freshDraftFromExistingHasNoUnsavedChanges() {
        val original = announcementDraftFrom(existing)
        val current = announcementDraftFrom(existing)
        assertFalse(hasUnsavedAnnouncementChanges(original, current))
    }

    @Test
    fun editingTitleIsDetectedAsUnsaved() {
        val original = announcementDraftFrom(existing)
        val current = original.copy(title = "Culto Jovem - Edição")
        assertTrue(hasUnsavedAnnouncementChanges(original, current))
    }

    // --- troca de imagem / remoção de imagem ---

    @Test
    fun removingImageIsDetectedAsUnsaved() {
        val original = announcementDraftFrom(existing)
        val current = original.copy(mediaType = MediaType.NONE, mediaUrl = null, mediaFileName = null)
        assertTrue(hasUnsavedAnnouncementChanges(original, current))
    }

    @Test
    fun swappingImageUrlIsDetectedAsUnsaved() {
        val original = announcementDraftFrom(existing)
        val current = original.copy(mediaUrl = "https://example.com/new-image.jpg")
        assertTrue(hasUnsavedAnnouncementChanges(original, current))
    }

    // --- publicar/despublicar via o mesmo draft ---

    @Test
    fun togglingPublishedStateIsDetectedAsUnsaved() {
        val original = announcementDraftFrom(existing)
        val current = original.copy(isActive = false)
        assertTrue(hasUnsavedAnnouncementChanges(original, current))
    }

    @Test
    fun newAnnouncementDefaultsToPublished() {
        val draft = announcementDraftFrom(null)
        assertTrue(draft.isActive)
    }

    // --- construção do Announcement final ---

    @Test
    fun buildFromDraftPreservesIdAndPublishedAtOnEdit() {
        val draft = announcementDraftFrom(existing).copy(title = "Novo título")
        val result = buildAnnouncementFromDraft(existing, draft)
        assertEquals(existing.id, result.id)
        assertEquals(existing.publishedAt, result.publishedAt)
        assertEquals("Novo título", result.title)
    }

    @Test
    fun buildFromDraftAssignsEmptyIdForNewAnnouncement() {
        val draft = announcementDraftFrom(null).copy(title = "Anúncio novo")
        val result = buildAnnouncementFromDraft(null, draft)
        assertEquals("", result.id)
        assertEquals("Anúncio novo", result.title)
    }
}
