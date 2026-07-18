package com.escalachurch.app.church

import com.escalachurch.app.domain.model.Church
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 11.9A hotfix - reproduces the exact bug: a brand-new install's ActiveChurchStore is empty,
 * exactly like a legacy install whose migration hasn't run yet. decideBootstrapOutcome() must tell
 * these apart using [LegacyMarker], never by the store's emptiness alone.
 */
class ActiveChurchManagerBootstrapTest {

    private val church = Church(id = "church-1", slug = "igreja-teste", name = "Igreja Teste", isActive = true)

    /** A. Fresh install: empty store, migration never evaluated, no legacy marker of any kind,
     *  BuildConfig may still be filled in (that's simulated by migratedChurch staying null even
     *  though a marker-free caller would never even look it up) - must be NeedsChurchEntry, never
     *  HasActiveChurch. This is the case that regressed in the original Fase 11.9A. */
    @Test
    fun freshInstall_noMarker_needsChurchEntry() {
        val outcome = decideBootstrapOutcome(
            storedChurch = null,
            alreadyEvaluated = false,
            marker = LegacyMarker.None,
            migratedChurch = null
        )
        assertEquals(BootstrapState.NeedsChurchEntry, outcome)
    }

    /** Same as A, but the one-time evaluation already happened before (second cold start of the
     *  same fresh install) - must still be NeedsChurchEntry, never re-migrate or flip to Home. */
    @Test
    fun freshInstall_alreadyEvaluated_staysNeedsChurchEntry() {
        val outcome = decideBootstrapOutcome(
            storedChurch = null,
            alreadyEvaluated = true,
            marker = LegacyMarker.None,
            migratedChurch = null
        )
        assertEquals(BootstrapState.NeedsChurchEntry, outcome)
    }

    /** B. Legacy install: empty store, migration not yet evaluated, a real marker was found and
     *  successfully resolved to a real church - must migrate once and land on HasActiveChurch. */
    @Test
    fun legacyInstall_withMarker_migratesOnce() {
        val outcome = decideBootstrapOutcome(
            storedChurch = null,
            alreadyEvaluated = false,
            marker = LegacyMarker.LegacyLocalData,
            migratedChurch = church
        )
        assertEquals(BootstrapState.HasActiveChurch(church), outcome)
    }

    /** A marker was found, but the church it pointed at couldn't actually be resolved (deleted,
     *  inactive, offline) - must not fabricate a church, falls through to entry. */
    @Test
    fun legacyInstall_markerButNoResolvedChurch_needsChurchEntry() {
        val outcome = decideBootstrapOutcome(
            storedChurch = null,
            alreadyEvaluated = false,
            marker = LegacyMarker.AuthenticatedProfile(churchId = "church-1"),
            migratedChurch = null
        )
        assertEquals(BootstrapState.NeedsChurchEntry, outcome)
    }

    /** C. A persisted active church always wins outright, regardless of every other input -
     *  covers normal reopen and "second cold start after a successful migration or code entry." */
    @Test
    fun persistedChurch_alwaysWinsRegardlessOfOtherInputs() {
        val outcome = decideBootstrapOutcome(
            storedChurch = church,
            alreadyEvaluated = false,
            marker = LegacyMarker.None,
            migratedChurch = null
        )
        assertTrue(outcome is BootstrapState.HasActiveChurch)
        assertEquals(church, (outcome as BootstrapState.HasActiveChurch).church)
    }
}
