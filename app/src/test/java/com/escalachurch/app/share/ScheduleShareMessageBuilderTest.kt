package com.escalachurch.app.share

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleShareMessageBuilderTest {

    @Test
    fun containsChurchNameCodeAndPublicUrl() {
        val message = buildScheduleShareMessage(
            churchName = "IASD Ariston",
            churchCode = "iasd-ariston",
            publicUrl = "https://escalachurch.app/c/iasd-ariston",
            scheduleTitle = "Culto de Sábado",
            dateLabel = "sábado, 25 de julho",
            timeLabel = "09:00"
        )
        assertTrue(message.contains("IASD Ariston"))
        assertTrue(message.contains("https://escalachurch.app/c/iasd-ariston"))
        assertTrue(message.contains("iasd-ariston"))
        assertTrue(message.contains("Culto de Sábado"))
    }

    @Test
    fun fallsBackToGenericTitleWhenScheduleTitleBlank() {
        val message = buildScheduleShareMessage("Igreja", "code", "https://escalachurch.app/c/code", "", "hoje", "")
        assertTrue(message.contains("Próxima programação"))
    }

    @Test
    fun neverPrintsNullOrEmptyTimeField() {
        val message = buildScheduleShareMessage("Igreja", "code", "https://escalachurch.app/c/code", "Culto", "hoje", "")
        assertFalse(message.contains("null"))
        assertFalse(message.contains(" — "))
    }

    @Test
    fun neverContainsChurchIdOrTokenLookingStrings() {
        val message = buildScheduleShareMessage("Igreja", "CODE", "https://escalachurch.app/c/code", "Culto", "hoje", "09:00")
        assertFalse(message.contains("church_id"))
        assertFalse(message.contains("Bearer"))
        assertFalse(message.contains("eyJ"))
    }
}
