package com.escalachurch.app.ui.components

import com.escalachurch.app.domain.model.Church
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareChurchAccessViewModelTest {

    private val configuredBase = "https://escalachurch.app"

    @Test
    fun activeChurch_producesFullyResolvedState() {
        val church = Church(id = "internal-uuid", slug = "iasd-ariston", name = "IASD Ariston", isActive = true)
        val state = buildShareChurchAccessUiState(church, configuredBase)

        assertEquals("IASD Ariston", state.churchName)
        assertEquals("iasd-ariston", state.churchCode)
        assertEquals("https://escalachurch.app/c/iasd-ariston", state.publicUrl)
        assertTrue(state.shareMessage.contains("https://escalachurch.app/c/iasd-ariston"))
        assertTrue(state.shareMessage.contains("iasd-ariston"))
        assertNull(state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun neverIncludesChurchIdAnywhereInTheResolvedState() {
        val church = Church(id = "internal-uuid-should-never-leak", slug = "iasd-ariston", name = "IASD Ariston", isActive = true)
        val state = buildShareChurchAccessUiState(church, configuredBase)

        assertEquals(false, state.publicUrl.contains("internal-uuid-should-never-leak"))
        assertEquals(false, state.shareMessage.contains("internal-uuid-should-never-leak"))
    }

    @Test
    fun switchingChurch_neverKeepsThePreviousLinkOrCode() {
        val churchA = Church(id = "a", slug = "igreja-a", name = "Igreja A", isActive = true)
        val churchB = Church(id = "b", slug = "igreja-b", name = "Igreja B", isActive = true)

        val stateA = buildShareChurchAccessUiState(churchA, configuredBase)
        val stateB = buildShareChurchAccessUiState(churchB, configuredBase)

        assertEquals("https://escalachurch.app/c/igreja-a", stateA.publicUrl)
        assertEquals("https://escalachurch.app/c/igreja-b", stateB.publicUrl)
        assertEquals(false, stateB.publicUrl.contains("igreja-a"))
        assertEquals(false, stateB.shareMessage.contains("Igreja A"))
    }

    @Test
    fun noActiveChurch_producesFriendlyError_notNullOrCrash() {
        val state = buildShareChurchAccessUiState(null, configuredBase)
        assertEquals("", state.publicUrl)
        assertEquals("Não foi possível gerar o link público desta igreja. Verifique o cadastro da igreja.", state.errorMessage)
    }

    @Test
    fun churchWithBlankSlug_producesFriendlyError_notABrokenLink() {
        val church = Church(id = "a", slug = "", name = "Igreja Incompleta", isActive = true)
        val state = buildShareChurchAccessUiState(church, configuredBase)
        assertEquals("", state.publicUrl)
        assertTrue(state.errorMessage!!.contains("cadastro"))
    }

    @Test
    fun missingDomainConfig_producesFriendlyError_butKeepsChurchCode() {
        val church = Church(id = "a", slug = "iasd-ariston", name = "IASD Ariston", isActive = true)
        val state = buildShareChurchAccessUiState(church, baseUrl = "")
        assertEquals("", state.publicUrl)
        assertEquals("iasd-ariston", state.churchCode)
        assertEquals("Endereço público do site não configurado.", state.errorMessage)
    }
}
