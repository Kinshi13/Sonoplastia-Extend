package com.escalachurch.app.security

import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.domain.model.AccessLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

/**
 * Local, PIN-protected "modo administrador" gate.
 *
 * TODO(auth): this is a deliberately simple, temporary MVP mechanism - a 4+ digit PIN hashed
 * and stored on-device. It is NOT real authentication (anyone with the PIN on any device could
 * unlock it, and the PIN never leaves the device). Replace with Firebase Auth / Supabase Auth /
 * API própria issuing verified roles before this app is used beyond a single trusted group.
 *
 * Unlocking is a per-process session (not persisted across app restarts) so admin mode always
 * requires re-entering the PIN when the app is reopened.
 */
class AdminSession(
    private val settingsRepository: SettingsRepository,
    private val userProfileRepository: UserProfileRepository
) {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    suspend fun hasPinConfigured(): Boolean = settingsRepository.settingsFlow.first().adminPinHash != null

    suspend fun setPin(pin: String) {
        settingsRepository.update(settingsRepository.settingsFlow.first().copy(adminPinHash = hash(pin)))
    }

    suspend fun unlock(pin: String): Boolean {
        val storedHash = settingsRepository.settingsFlow.first().adminPinHash ?: return false
        val matches = storedHash == hash(pin)
        if (matches) {
            _isUnlocked.value = true
            userProfileRepository.setAccessLevel(AccessLevel.ADMIN)
        }
        return matches
    }

    suspend fun lock() {
        _isUnlocked.value = false
        userProfileRepository.setAccessLevel(AccessLevel.MEMBER)
    }

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
