package com.escalachurch.app.security

import com.escalachurch.app.church.ActiveChurchManager
import com.escalachurch.app.data.repository.ChurchRepository
import com.escalachurch.app.data.repository.PlanRepository
import com.escalachurch.app.data.repository.ProfileRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.domain.model.AccessLevel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * "Modo administrador" gate, backed by real Supabase Auth (email/password) + the `profiles.is_admin`
 * flag (row-level security in supabase/schema.sql enforces this server-side too, this is just the
 * UI-side mirror). Only accounts created by whoever manages the Supabase project (Dashboard ->
 * Authentication -> Add user, then promote via the SQL at the bottom of schema.sql) can become
 * admins - there is no self-serve sign-up screen, so a random member can never grant themselves
 * admin rights from the app.
 *
 * Fase 11.9A: on success, resolves the account's own `profiles.church_id` and sets it as the
 * active church (see ActiveChurchManager) - the flow the spec calls for is "login -> profile.
 * church_id -> definir igreja ativa -> carregar PRO", not a separate admin-only plan lookup. That
 * means logging in as Admin can switch which church the whole app (including public-view content)
 * is pointed at, matching "Admin continua vinculado à mesma igreja" - never a different one than
 * the account actually belongs to, regardless of which church this device was last viewing.
 */
class AdminSession(
    private val client: SupabaseClient,
    private val userProfileRepository: UserProfileRepository,
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val churchRepository: ChurchRepository,
    private val activeChurchManager: ActiveChurchManager
) {
    private val _isUnlocked = MutableStateFlow(client.auth.currentUserOrNull() != null)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    suspend fun signIn(email: String, password: String): Result<Unit> {
        val result = runCatching {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = client.auth.currentUserOrNull()?.id
                ?: error("Sessão não confirmada após o login.")
            val churchId = profileRepository.fetchChurchId(userId)
                ?: error("Esta conta ainda não está vinculada a nenhuma igreja.")
            val church = churchRepository.findById(churchId)
                ?: error("A igreja vinculada a esta conta não foi encontrada.")
            activeChurchManager.setActiveChurch(church)
            userProfileRepository.setAccessLevel(AccessLevel.ADMIN)
        }
        if (result.isSuccess) {
            _isUnlocked.value = true
            // ActiveChurchManager.activeChurchId changing already re-triggers PlanRepository's
            // flatMapLatest, but this forces it immediately rather than waiting on that emission,
            // so entitlements never briefly show stale/FREE right after a successful login.
            planRepository.refresh()
        } else {
            // Auth may have already succeeded before a later step failed (e.g. an account with no
            // church) - never leave a signed-in Supabase session that the app itself considers
            // logged out, that mismatch is worse than just signing back out.
            runCatching { client.auth.signOut() }
        }
        return result
    }

    suspend fun signOut() {
        client.auth.signOut()
        _isUnlocked.value = false
        userProfileRepository.setAccessLevel(AccessLevel.MEMBER)
        // The active church itself is deliberately left untouched - logging out of Admin must
        // still let the same device keep viewing that church in public mode (Fase 11.9A item 12).
        planRepository.refresh()
    }
}
