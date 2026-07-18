package com.escalachurch.app.church

import com.escalachurch.app.BuildConfig
import com.escalachurch.app.data.preferences.ActiveChurchStore
import com.escalachurch.app.data.preferences.RecentChurchStore
import com.escalachurch.app.data.repository.ChurchRepository
import com.escalachurch.app.domain.model.Church
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Fase 11.9A - single source of truth for "which church is this session pointed at right now."
 * Every repository that used to read BuildConfig.CHURCH_ID directly (scales, doxologies,
 * announcements, plan/subscription) now reacts to [activeChurchId] instead, so switching churches
 * immediately switches what they show - see ScaleRepository/DoxologyRepository/
 * AnnouncementRepository/PlanRepository.
 *
 * BuildConfig.CHURCH_ID is touched in exactly one place: [ensureBootstrapped], and only when there
 * is no persisted active church yet (a fresh ActiveChurchStore). That's the one-time migration
 * path for installs upgrading from before this phase, which were compiled pinned to one church and
 * have no other way to know which - see 002_multi_tenant.sql's own comment about this. Once a
 * church has been resolved once (by that migration or by ChurchEntryScreen/Admin login), it's
 * persisted and BuildConfig is never consulted again.
 */
class ActiveChurchManager(
    private val store: ActiveChurchStore,
    private val churchRepository: ChurchRepository,
    private val recentChurchStore: RecentChurchStore,
    scope: CoroutineScope
) {
    private val _activeChurch = MutableStateFlow<Church?>(null)
    val activeChurch: StateFlow<Church?> = _activeChurch

    /** True once the initial resolution (persisted church, or the BuildConfig migration) has
     *  finished - whether or not it found a church. The entry screen should only decide whether to
     *  show itself once this is true, so a user with a valid saved church never flashes the entry
     *  screen first (see NavGraph). */
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    /** Only emits once there's a real church - repositories that depend on it (via flatMapLatest)
     *  naturally wait for that instead of ever querying with an empty/wrong church_id. Screens that
     *  read these repositories are only reachable once NavGraph has already gated on isReady +
     *  activeChurch != null, so in practice this never blocks a composed screen for long. */
    val activeChurchId: Flow<String> = activeChurch.filterNotNull().map { it.id }.distinctUntilChanged()

    init {
        scope.launch { ensureBootstrapped() }
    }

    private suspend fun ensureBootstrapped() {
        val stored = store.activeChurchFlow.first()
        if (stored != null) {
            _activeChurch.value = stored
        } else if (BuildConfig.CHURCH_ID.isNotBlank()) {
            val migrated = churchRepository.findById(BuildConfig.CHURCH_ID)
            if (migrated != null && migrated.isActive) {
                setActiveChurch(migrated)
            }
        }
        _isReady.value = true
    }

    /** Called by ChurchEntryScreen (code entry / "Continuar em" / switch) and by AdminSession
     *  after resolving profile.church_id - the one place that changes what "active" means. */
    suspend fun setActiveChurch(church: Church) {
        store.save(church)
        recentChurchStore.recordAccess(church.id, church.slug, church.name)
        _activeChurch.value = church
    }
}
