package com.escalachurch.app.security

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
 */
class AdminSession(
    private val client: SupabaseClient,
    private val userProfileRepository: UserProfileRepository
) {
    private val _isUnlocked = MutableStateFlow(client.auth.currentUserOrNull() != null)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        _isUnlocked.value = true
        userProfileRepository.setAccessLevel(AccessLevel.ADMIN)
    }

    suspend fun signOut() {
        client.auth.signOut()
        _isUnlocked.value = false
        userProfileRepository.setAccessLevel(AccessLevel.MEMBER)
    }
}
