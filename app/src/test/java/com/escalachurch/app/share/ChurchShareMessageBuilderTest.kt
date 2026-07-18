package com.escalachurch.app.share

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChurchShareMessageBuilderTest {

    @Test
    fun containsChurchNameCodeAndFullPublicUrl_verifiably() {
        val message = buildChurchShareMessage(
            churchName = "IASD Ariston",
            churchCode = "IASD-ARISTON",
            publicUrl = "https://escalachurch.app/c/iasd-ariston"
        )
        assertTrue(message.contains("IASD Ariston"))
        assertTrue(message.contains("https://escalachurch.app/c/iasd-ariston"))
        assertTrue(message.contains("IASD-ARISTON"))
    }

    @Test
    fun neverContainsChurchIdOrTokenLookingStrings() {
        val message = buildChurchShareMessage("Igreja", "CODE", "https://escalachurch.app/c/code")
        assertFalse(message.contains("church_id"))
        assertFalse(message.contains("Bearer"))
        assertFalse(message.contains("eyJ")) // common JWT prefix
    }

    @Test
    fun neverContainsPlaceholderText() {
        val message = buildChurchShareMessage("IASD Ariston", "IASD-ARISTON", "https://escalachurch.app/c/iasd-ariston")
        assertFalse(message.contains("SEUDOMINIO"))
        assertFalse(message.contains("SLUG_DA_IGREJA"))
        assertFalse(message.contains("CODIGO_DA_IGREJA"))
        assertFalse(message.contains("Igreja Exemplo"))
    }
}
