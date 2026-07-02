package com.escalachurch.app.domain.model

/** Ministry "classes" a member can belong to, used to filter/highlight relevant info. */
enum class UserClass(val label: String) {
    SONOPLASTA("Sonoplasta"),
    REGENTE("Regente"),
    CANTOR("Cantor"),
    PREGADOR("Pregador"),
    RECEPCIONISTA("Recepcionista")
}

enum class AccessLevel {
    ADMIN,
    MEMBER
}

/** Where a piece of data came from: admin-managed (shared truth) vs a member's own personal item. */
enum class SourceType {
    OFFICIAL,
    PERSONAL
}

/**
 * Local user profile. Today this is a single on-device profile (no accounts), persisted via
 * DataStore - see UserProfileDataStore. [accessLevel] can only become ADMIN through the local
 * PIN-protected admin mode (see AdminSession), never by editing this model directly from
 * Configurações, so a regular member can't grant themselves admin rights.
 *
 * TODO(auth): once a real backend exists (Firebase Auth / Supabase Auth / API própria), this
 * profile should be rehydrated from the authenticated account instead of local-only storage,
 * and accessLevel should be issued by the backend (custom claims / role table), not the device.
 */
data class UserProfile(
    val id: String = "local-user",
    val name: String = "",
    val selectedClasses: Set<UserClass> = emptySet(),
    val accessLevel: AccessLevel = AccessLevel.MEMBER,
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
