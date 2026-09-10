package com.escalachurch.app.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class NameMatcherTest {

    private val knownNames = listOf("Robson", "Ana Paula", "João Pedro", "Maria")

    @Test
    fun `exact match, case and accent insensitive`() {
        assertEquals(NameMatch.Exact("Robson"), NameMatcher.match("robson", knownNames))
        assertEquals(NameMatch.Exact("Ana Paula"), NameMatcher.match("ANA PAULA", knownNames))
    }

    @Test
    fun `typo produces a suggestion, never a silent substitution`() {
        val result = NameMatcher.match("Robsonn", knownNames)
        assertEquals(NameMatch.Suggestion("Robson"), result)
    }

    @Test
    fun `unrelated name is not found`() {
        assertEquals(NameMatch.NotFound, NameMatcher.match("Fulano de Tal", knownNames))
    }

    @Test
    fun `empty people bank never matches`() {
        assertEquals(NameMatch.NotFound, NameMatcher.match("Robson", emptyList()))
    }

    @Test
    fun `short unrelated names do not accidentally match each other`() {
        // "Ana" vs "Maria" shouldn't be treated as a typo of one another.
        assertEquals(NameMatch.NotFound, NameMatcher.match("Ana", listOf("Maria")))
    }
}
