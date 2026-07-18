package com.escalachurch.app.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChurchShareLinkBuilderTest {

    @Test
    fun buildsLinkUsingRealPublicRoute_notTheIgrejaGuess() {
        assertEquals("https://escalachurch.app/c/iasd-ariston", buildPublicChurchUrl("iasd-ariston", "https://escalachurch.app"))
    }

    @Test
    fun trailingSlashOnBaseUrl_doesNotProduceDoubleSlash() {
        assertEquals("https://escalachurch.app/c/iasd-ariston", buildPublicChurchUrl("iasd-ariston", "https://escalachurch.app/"))
    }

    @Test
    fun slugWithExtraSpacesOrSlashes_isNormalized() {
        assertEquals("https://escalachurch.app/c/iasd-ariston", buildPublicChurchUrl("  iasd-ariston  ", "https://escalachurch.app"))
        assertEquals("https://escalachurch.app/c/iasd-ariston", buildPublicChurchUrl("/iasd-ariston/", "https://escalachurch.app"))
    }

    @Test
    fun blankSlug_returnsNull_neverABrokenLink() {
        assertNull(buildPublicChurchUrl("", "https://escalachurch.app"))
        assertNull(buildPublicChurchUrl("   ", "https://escalachurch.app"))
    }

    @Test
    fun blankBaseUrl_returnsNull_neverARelativePath() {
        assertNull(buildPublicChurchUrl("iasd-ariston", ""))
    }

    @Test
    fun everyNonNullResult_isAnAbsoluteUrl() {
        val url = buildPublicChurchUrl("iasd-ariston", "https://escalachurch.app")
        requireNotNull(url)
        assertTrue(url.startsWith("http://") || url.startsWith("https://"))
    }

    @Test
    fun neverIncludesAnythingOtherThanTheSlug() {
        val link = buildPublicChurchUrl("iasd-ariston", "https://escalachurch.app")
        requireNotNull(link)
        assertEquals(false, link.contains("church_id"))
        assertEquals(false, link.contains("uuid", ignoreCase = true))
    }
}
