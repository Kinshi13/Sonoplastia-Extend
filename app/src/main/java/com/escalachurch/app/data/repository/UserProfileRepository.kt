package com.escalachurch.app.data.repository

import com.escalachurch.app.data.preferences.UserProfileDataStore
import com.escalachurch.app.domain.model.AccessLevel
import com.escalachurch.app.domain.model.UserClass
import com.escalachurch.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class UserProfileRepository(private val dataStore: UserProfileDataStore) {

    val profileFlow: Flow<UserProfile> = dataStore.profileFlow

    suspend fun updateClasses(classes: Set<UserClass>) {
        val current = dataStore.profileFlow.first()
        dataStore.update(current.copy(selectedClasses = classes, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateName(name: String) {
        val current = dataStore.profileFlow.first()
        dataStore.update(current.copy(name = name, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        val current = dataStore.profileFlow.first()
        dataStore.update(current.copy(notificationsEnabled = enabled, updatedAt = System.currentTimeMillis()))
    }

    /** Only [com.escalachurch.app.security.AdminSession] should call this - see its docs. */
    internal suspend fun setAccessLevel(level: AccessLevel) {
        val current = dataStore.profileFlow.first()
        dataStore.update(current.copy(accessLevel = level, updatedAt = System.currentTimeMillis()))
    }
}
