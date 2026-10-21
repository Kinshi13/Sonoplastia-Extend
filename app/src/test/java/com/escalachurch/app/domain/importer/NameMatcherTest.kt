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

    // Fase 11.15 - the exact scenario Fase 11.13's audit called out: a bare first name that
    // matches more than one full name on file must never resolve to just one of them.
    private val threeJoaos = listOf("João Pedro Almeida", "João Carlos Ferreira", "João Batista Santos")

    @Test
    fun `a first name matching several known people is Ambiguous, never auto-picked`() {
        val result = NameMatcher.match("João", threeJoaos)
        assertEquals(NameMatch.Ambiguous(threeJoaos), result)
    }

    @Test
    fun `a first name matching exactly one known person is a Suggestion`() {
        assertEquals(NameMatch.Suggestion("João Pedro Almeida"), NameMatcher.match("João", listOf("João Pedro Almeida", "Maria")))
    }

    @Test
    fun `a full name still resolves normally when only one first-name collision exists`() {
        // "João Pedro Almeida" (full, with space) skips the first-name branch entirely and still
        // exact-matches - the Ambiguous path must never swallow a fully-qualified name.
        assertEquals(NameMatch.Exact("João Pedro Almeida"), NameMatcher.match("João Pedro Almeida", threeJoaos))
    }

    @Test
    fun `accents and case do not affect the ambiguity check`() {
        assertEquals(NameMatch.Ambiguous(threeJoaos), NameMatcher.match("JOAO", threeJoaos))
    }
}
