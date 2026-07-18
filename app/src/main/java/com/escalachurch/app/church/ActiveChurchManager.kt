package com.escalachurch.app.church

import android.util.Log
import com.escalachurch.app.BuildConfig
import com.escalachurch.app.data.preferences.ActiveChurchStore
import com.escalachurch.app.data.preferences.RecentChurchStore
import com.escalachurch.app.data.repository.ChurchRepository
import com.escalachurch.app.domain.model.Church
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val LOG_TAG = "ChurchBootstrap"

/**
 * The actual bootstrap decision, pulled out as a pure function so the exact bug this hotfix
 * targets - "an empty ActiveChurchStore alone was wrongly treated as proof of a legacy install" -
 * can be unit-tested without any Android/network dependency. See
 * ActiveChurchManagerBootstrapTest for the fresh-install and legacy-install cases this must get
 * right.
 *
 * A persisted [storedChurch] always wins. Otherwise, only a real [migratedChurch] (itself only
 * ever non-null when [marker] is not [LegacyMarker.None] - see ActiveChurchManager.runBootstrap)
 * can produce [BootstrapState.HasActiveChurch]; everything else - including a plain empty store
 * with no marker at all, which is exactly what a brand-new install looks like - lands on
 * [BootstrapState.NeedsChurchEntry].
 */
internal fun decideBootstrapOutcome(
    storedChurch: Church?,
    alreadyEvaluated: Boolean,
    marker: LegacyMarker,
    migratedChurch: Church?
): BootstrapState {
    if (storedChurch != null) return BootstrapState.HasActiveChurch(storedChurch)
    if (alreadyEvaluated) return BootstrapState.NeedsChurchEntry
    return migratedChurch?.let { BootstrapState.HasActiveChurch(it) } ?: BootstrapState.NeedsChurchEntry
}

/**
 * Fase 11.9A - single source of truth for "which church is this session pointed at right now."
 * Every repository that used to read BuildConfig.CHURCH_ID directly (scales, doxologies,
 * announcements, plan/subscription) reacts to [activeChurchId] instead, so switching churches
 * immediately switches what they show.
 *
 * Hotfix: the original version of this class treated "ActiveChurchStore has nothing persisted
 * yet" as proof of a legacy (pre-11.9A) install and auto-adopted BuildConfig.CHURCH_ID - but a
 * brand-new install's store is equally empty, so it was skipping ChurchEntryScreen for everyone.
 * BuildConfig is now only ever consulted through [LegacyChurchMigration], which requires actual
 * evidence (an authenticated session that resolves a real church server-side, or a DataStore file
 * that predates this phase) before it's trusted - see [runBootstrap].
 */
class ActiveChurchManager(
    private val store: ActiveChurchStore,
    private val churchRepository: ChurchRepository,
    private val recentChurchStore: RecentChurchStore,
    private val legacyChurchMigration: LegacyChurchMigration,
    scope: CoroutineScope
) {
    private val _bootstrapState = MutableStateFlow<BootstrapState>(BootstrapState.Loading)
    val bootstrapState: StateFlow<BootstrapState> = _bootstrapState

    /** UI convenience - the same church as [bootstrapState]'s HasActiveChurch case, or null. */
    val activeChurch: StateFlow<Church?> = _bootstrapState
        .map { (it as? BootstrapState.HasActiveChurch)?.church }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Only emits once there's a real church - repositories that depend on it (via flatMapLatest)
     *  naturally wait for that. Screens that read those repositories are only reachable once
     *  NavGraph has already gated on bootstrapState being HasActiveChurch, so in practice this
     *  never blocks a composed screen. */
    val activeChurchId: Flow<String> = _bootstrapState
        .filterIsInstance<BootstrapState.HasActiveChurch>()
        .map { it.church.id }
        .distinctUntilChanged()

    init {
        scope.launch { runBootstrap() }
    }

    private suspend fun runBootstrap() {
        val stored = store.activeChurchFlow.first()
        val recentCount = recentChurchStore.recentChurchesFlow.first().size
        val alreadyEvaluated = if (stored == null) store.isMigrationEvaluated() else false

        var marker: LegacyMarker = LegacyMarker.None
        var migratedChurch: Church? = null
        if (stored == null && !alreadyEvaluated) {
            marker = runCatching { legacyChurchMigration.findMarker() }.getOrDefault(LegacyMarker.None)
            val legacyChurchId = when (marker) {
                is LegacyMarker.AuthenticatedProfile -> marker.churchId
                LegacyMarker.LegacyLocalData -> legacyChurchMigration.legacyChurchId()
                LegacyMarker.None -> null
            }
            migratedChurch = legacyChurchId?.let { runCatching { churchRepository.findById(it) }.getOrNull() }
                ?.takeIf { it.isActive }
            store.markMigrationEvaluated()
        }

        val outcome = decideBootstrapOutcome(
            storedChurch = stored,
            alreadyEvaluated = alreadyEvaluated,
            marker = marker,
            migratedChurch = migratedChurch
        )

        logBootstrap(
            activeChurchFound = stored != null,
            recentChurchCount = recentCount,
            legacyMarkerFound = marker != LegacyMarker.None,
            migrationApplied = outcome is BootstrapState.HasActiveChurch && stored == null,
            buildConfigFallbackUsed = marker == LegacyMarker.LegacyLocalData && migratedChurch != null,
            resultingBootstrapState = when (outcome) {
                is BootstrapState.HasActiveChurch -> "HasActiveChurch"
                is BootstrapState.NeedsChurchEntry -> "NeedsChurchEntry"
                is BootstrapState.Loading -> "Loading"
                is BootstrapState.Error -> "Error"
            },
            initialRoute = if (outcome is BootstrapState.HasActiveChurch) "Home" else "ChurchEntry"
        )

        if (outcome is BootstrapState.HasActiveChurch && stored == null) {
            // A fresh migration (not just a previously-persisted church) still needs to be saved
            // and recorded as a recent church, same as any other setActiveChurch() call.
            setActiveChurch(outcome.church)
        } else {
            _bootstrapState.value = outcome
        }
    }

    /** Called by ChurchEntryScreen (code entry / "Continuar em" / switch) and by AdminSession
     *  after resolving profile.church_id - the one place that changes what "active" means. */
    suspend fun setActiveChurch(church: Church) {
        store.save(church)
        recentChurchStore.recordAccess(church.id, church.slug, church.name)
        _bootstrapState.value = BootstrapState.HasActiveChurch(church)
    }

    private fun logBootstrap(
        activeChurchFound: Boolean,
        recentChurchCount: Int,
        legacyMarkerFound: Boolean,
        migrationApplied: Boolean,
        buildConfigFallbackUsed: Boolean,
        resultingBootstrapState: String,
        initialRoute: String
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            LOG_TAG,
            "activeChurchFound=$activeChurchFound recentChurchCount=$recentChurchCount " +
                "legacyMarkerFound=$legacyMarkerFound migrationApplied=$migrationApplied " +
                "buildConfigFallbackUsed=$buildConfigFallbackUsed resultingBootstrapState=$resultingBootstrapState " +
                "initialRoute=$initialRoute"
        )
    }
}
