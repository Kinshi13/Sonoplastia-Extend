package com.escalachurch.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeLinkParserTest {

    @Test
    fun `watch url`() {
        assertEquals("dQw4w9WgXcQ", YoutubeLinkParser.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `watch url with extra params`() {
        assertEquals("dQw4w9WgXcQ", YoutubeLinkParser.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=abc&t=30s"))
    }

    @Test
    fun `short url`() {
        assertEquals("dQw4w9WgXcQ", YoutubeLinkParser.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun `music youtube url`() {
        assertEquals("dQw4w9WgXcQ", YoutubeLinkParser.extractVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `shorts url`() {
        assertEquals("dQw4w9WgXcQ", YoutubeLinkParser.extractVideoId("https://youtube.com/shorts/dQw4w9WgXcQ"))
    }

    @Test
    fun `non-youtube url returns null`() {
        assertNull(YoutubeLinkParser.extractVideoId("https://example.com/video"))
    }

    @Test
    fun `blank url returns null`() {
        assertNull(YoutubeLinkParser.extractVideoId(""))
    }
}
