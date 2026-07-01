package com.escalachurch.app.data.sync

import com.escalachurch.app.domain.model.Workspace
import com.escalachurch.app.domain.model.WorkspaceMember
import com.escalachurch.app.domain.model.WorkspaceRole

/**
 * Seam between the app and a future shared backend (the recommended path is
 * Firebase: Firestore for data, Firebase Auth for invite-code login, FCM for
 * push notifications - see README "Modo igreja / multiusuário").
 *
 * The local Room database keeps being the source the UI reads from
 * (offline-first); a real implementation of this gateway would push admin
 * writes to the backend and pull remote changes back into Room, while
 * [LocalOnlySyncGateway] simply keeps the app in standalone mode.
 *
 * Wiring a real backend requires: creating a Firebase project, enabling
 * Firestore + Authentication + Cloud Messaging, dropping google-services.json
 * into app/, and adding a FirebaseScheduleSyncGateway implementation of this
 * interface - no other call site should need to change.
 */
interface ScheduleSyncGateway {

    suspend fun createWorkspace(name: String, adminDisplayName: String): Result<Workspace>

    suspend fun joinWorkspace(inviteCode: String, displayName: String): Result<WorkspaceMember>

    suspend fun leaveWorkspace(): Result<Unit>

    suspend fun currentRole(): WorkspaceRole?
}

/** Default gateway while no backend is configured: the app stays fully local/standalone. */
class LocalOnlySyncGateway : ScheduleSyncGateway {

    override suspend fun createWorkspace(name: String, adminDisplayName: String): Result<Workspace> =
        Result.failure(UnsupportedOperationException("Conecte um backend (ex: Firebase) para habilitar o modo igreja."))

    override suspend fun joinWorkspace(inviteCode: String, displayName: String): Result<WorkspaceMember> =
        Result.failure(UnsupportedOperationException("Conecte um backend (ex: Firebase) para habilitar o modo igreja."))

    override suspend fun leaveWorkspace(): Result<Unit> = Result.success(Unit)

    override suspend fun currentRole(): WorkspaceRole? = null
}
