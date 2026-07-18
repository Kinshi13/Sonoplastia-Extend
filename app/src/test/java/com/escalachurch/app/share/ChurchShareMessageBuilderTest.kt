package com.escalachurch.app.share

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChurchShareMessageBuilderTest {

    @Test
    fun includesChurchNameLinkAndUppercasedCode() {
        val message = buildChurchShareMessage(
            churchName = "IASD Ariston",
            churchSlug = "iasd-ariston",
            link = "https://escalachurch.app/c/iasd-ariston"
        )
        assertTrue(message.contains("IASD Ariston"))
        assertTrue(message.contains("https://escalachurch.app/c/iasd-ariston"))
        assertTrue(message.contains("IASD-ARISTON"))
    }

    @Test
    fun withoutNextScaleSummary_stillProducesAValidMessage() {
        val message = buildChurchShareMessage("Igreja", "slug", "https://escalachurch.app/c/slug")
        assertTrue(message.contains("Baixe também o aplicativo Escala Church"))
    }

    @Test
    fun withNextScaleSummary_includesIt() {
        val message = buildChurchShareMessage(
            "Igreja", "slug", "https://escalachurch.app/c/slug",
            nextScaleSummary = "Culto de Domingo · 15/09 · 19:00"
        )
        assertTrue(message.contains("Culto de Domingo · 15/09 · 19:00"))
    }

    @Test
    fun neverContainsChurchIdOrTokenLookingStrings() {
        val message = buildChurchShareMessage("Igreja", "slug", "https://escalachurch.app/c/slug")
        assertFalse(message.contains("church_id"))
        assertFalse(message.contains("Bearer"))
        assertFalse(message.contains("eyJ")) // common JWT prefix
    }
}
