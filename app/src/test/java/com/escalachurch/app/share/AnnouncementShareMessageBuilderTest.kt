package com.escalachurch.app.share

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementShareMessageBuilderTest {

    @Test
    fun containsTitleDescriptionChurchNameCodeAndPublicUrl() {
        val message = buildAnnouncementShareMessage(
            title = "Culto Jovem",
            description = "Venha participar do culto jovem deste sábado.",
            churchName = "IASD Ariston",
            churchCode = "iasd-ariston",
            publicUrl = "https://escalachurch.app/c/iasd-ariston"
        )
        assertTrue(message.contains("Culto Jovem"))
        assertTrue(message.contains("Venha participar do culto jovem deste sábado."))
        assertTrue(message.contains("IASD Ariston"))
        assertTrue(message.contains("https://escalachurch.app/c/iasd-ariston"))
        assertTrue(message.contains("iasd-ariston"))
    }

    @Test
    fun omitsDescriptionBlockWhenBlank() {
        val message = buildAnnouncementShareMessage(
            title = "Aviso rápido",
            description = "",
            churchName = "Igreja",
            churchCode = "code",
            publicUrl = "https://escalachurch.app/c/code"
        )
        assertFalse(message.contains("\n\n\n"))
    }

    @Test
    fun neverContainsChurchIdOrTokenLookingStrings() {
        val message = buildAnnouncementShareMessage("Título", "Texto", "Igreja", "CODE", "https://escalachurch.app/c/code")
        assertFalse(message.contains("church_id"))
        assertFalse(message.contains("Bearer"))
        assertFalse(message.contains("eyJ"))
    }
}
