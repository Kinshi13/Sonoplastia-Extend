package com.escalachurch.app.security

import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.domain.model.AccessLevel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/**
 * "Modo administrador" gate, backed by real Firebase Auth (email/password). Only accounts
 * created by whoever manages the Firebase project (Console → Authentication → Add user) can
 * sign in here - there is no self-serve sign-up screen, so a random member can never grant
 * themselves admin rights from the app.
 *
 * Firebase Auth persists the session across app restarts by default (same as most apps), so an
 * admin who signs in stays signed in until they explicitly sign out - unlike the old local PIN,
 * which required re-entry every time the app opened.
 */
class AdminSession(
    private val auth: FirebaseAuth,
    private val userProfileRepository: UserProfileRepository
) {
    private val _isUnlocked = MutableStateFlow(auth.currentUser != null)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _isUnlocked.value = firebaseAuth.currentUser != null
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        userProfileRepository.setAccessLevel(AccessLevel.ADMIN)
    }

    suspend fun signOut() {
        auth.signOut()
        userProfileRepository.setAccessLevel(AccessLevel.MEMBER)
    }
}
