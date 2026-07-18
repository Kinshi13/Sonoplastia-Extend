package com.escalachurch.app.ui.navigation

import com.escalachurch.app.church.BootstrapState
import com.escalachurch.app.domain.model.Church
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fase 11.9A NavGraph hotfix - routeFor() is the pure BootstrapState -> route mapping NavGraph
 * actually navigates with. No emulator/adb is available in this environment to run a full
 * instrumented Compose navigation test (see the hotfix report), so this is the closest verifiable
 * proof that NeedsChurchEntry can never resolve to the "app"/Home route, covering the exact
 * scenarios requested: empty stores/no auth/no legacy marker, a persisted active church, and an
 * Admin login that resolved a church.
 */
class NavGraphRouteTest {

    private val church = Church(id = "church-1", slug = "igreja-teste", name = "Igreja Teste", isActive = true)

    @Test
    fun freshInstall_needsChurchEntry_routesToChurchEntry() {
        assertEquals("church_entry", routeFor(BootstrapState.NeedsChurchEntry))
    }

    @Test
    fun persistedActiveChurch_routesToApp() {
        assertEquals("app", routeFor(BootstrapState.HasActiveChurch(church)))
    }

    @Test
    fun adminLoginResolvedChurch_routesToApp() {
        // Same state AdminSession.signIn() lands on after resolving profile.church_id and calling
        // ActiveChurchManager.setActiveChurch() - see AdminSession.kt.
        val stateAfterAdminLogin = BootstrapState.HasActiveChurch(church)
        assertEquals("app", routeFor(stateAfterAdminLogin))
    }

    @Test
    fun loading_routesToBootstrap_neverToApp() {
        assertEquals("bootstrap", routeFor(BootstrapState.Loading))
    }

    @Test
    fun error_routesToChurchEntry_neverToApp() {
        assertEquals("church_entry", routeFor(BootstrapState.Error("network")))
    }
}
