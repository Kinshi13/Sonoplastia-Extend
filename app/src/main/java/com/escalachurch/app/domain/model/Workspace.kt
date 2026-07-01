package com.escalachurch.app.domain.model

/**
 * A "church workspace": the shared space an admin manages and members join via
 * an invite code, once the app is connected to a backend (see [SyncMode]).
 * Kept in the domain layer, independent of whichever backend eventually
 * implements [com.escalachurch.app.data.sync.ScheduleSyncGateway].
 */
data class Workspace(
    val id: String,
    val name: String,
    val inviteCode: String
)

enum class WorkspaceRole {
    /** Creates/edits/deletes scales, doxologies and events for the workspace. */
    ADMIN,
    /** Read-only: views everything and gets notified when included in a scale. */
    MEMBER
}

data class WorkspaceMember(
    val userId: String,
    val displayName: String,
    val role: WorkspaceRole
)

/** Whether the app is running fully offline (today's default) or synced to a shared workspace. */
enum class SyncMode {
    STANDALONE,
    CHURCH_WORKSPACE
}
