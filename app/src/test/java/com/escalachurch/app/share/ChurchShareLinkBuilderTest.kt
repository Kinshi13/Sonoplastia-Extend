package com.escalachurch.app.share

import org.junit.Assert.assertEquals
import org.junit.Test

class ChurchShareLinkBuilderTest {

    @Test
    fun buildsLinkUsingRealPublicRoute_notTheOriginalSpecGuess() {
        assertEquals("https://escalachurch.app/c/iasd-ariston", buildChurchLink("https://escalachurch.app", "iasd-ariston"))
    }

    @Test
    fun trailingSlashOnBaseUrl_doesNotProduceDoubleSlash() {
        assertEquals("https://escalachurch.app/c/iasd-ariston", buildChurchLink("https://escalachurch.app/", "iasd-ariston"))
    }

    @Test
    fun blankBaseUrl_stillProducesARelativePath_doesNotCrash() {
        assertEquals("/c/iasd-ariston", buildChurchLink("", "iasd-ariston"))
    }

    @Test
    fun neverIncludesAnythingOtherThanTheSlug() {
        val link = buildChurchLink("https://escalachurch.app", "iasd-ariston")
        assertEquals(false, link.contains("church_id"))
        assertEquals(false, link.contains("uuid", ignoreCase = true))
    }
}
