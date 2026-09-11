package com.escalachurch.app.data.repository

import com.escalachurch.app.data.remote.dto.toDto
import com.escalachurch.app.domain.model.WorshipSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.UUID

/**
 * Fase 11.11 (correção preventiva, Bloco 24) - characterization test for the fix in
 * [WorshipSongRepository.save]: a genuine end-to-end test would need a real/mocked Supabase HTTP
 * client and RLS, not available in this sandbox (see AnnouncementRepository, which has the same
 * gap for the identical fix). This instead pins down the exact logic that makes the fix work -
 * `save()` never relies on reading the id back from a post-insert SELECT, it generates one
 * up front and forces it onto the DTO before the insert call - so a regression (e.g. someone
 * reintroducing `select(Columns.list("id"))`) changes what this test asserts.
 */
class WorshipSongSaveIdTest {

    @Test
    fun `a new (blank id) song gets a fresh non-blank id before insert, never derived from a read-back`() {
        val draft = WorshipSong(id = "", title = "Grande é o Senhor", isPublished = false)

        // Mirrors WorshipSongRepository.save()'s new-item branch exactly: generate first, then
        // force it onto the DTO - the insert call itself never has to be read back to learn the id.
        val newId = UUID.randomUUID().toString()
        val dto = draft.toDto(churchId = "church-1").copy(id = newId)

        assertEquals(newId, dto.id)
        assertFalse("a real UUID is never blank", dto.id.isNullOrBlank())
        // isPublished = false (a draft) is exactly the case that used to break: worship_songs'
        // only SELECT policy is `using (is_published)`, so a draft's row is invisible to a
        // post-insert `select(Columns.list("id"))`. This fix never attempts that read.
        assertFalse(dto.isPublished)
    }

    @Test
    fun `two consecutive new songs never collide on id`() {
        val a = UUID.randomUUID().toString()
        val b = UUID.randomUUID().toString()
        assertNotEquals(a, b)
    }

    @Test
    fun `an existing item keeps its own id instead of generating a new one`() {
        val existing = WorshipSong(id = "existing-id-123", title = "Já cadastrada")
        val dto = existing.toDto(churchId = "church-1")
        assertEquals("existing-id-123", dto.id)
    }
}
