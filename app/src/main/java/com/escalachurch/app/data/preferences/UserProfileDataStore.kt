package com.escalachurch.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.escalachurch.app.domain.model.AccessLevel
import com.escalachurch.app.domain.model.UserClass
import com.escalachurch.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userProfileDataStore by preferencesDataStore(name = "escala_church_user_profile")

/**
 * Local single-device [UserProfile] persistence.
 *
 * TODO(auth): once a backend exists, this becomes a cache of the authenticated
 * account's profile (name/classes/role synced from the server) instead of the
 * source of truth - accessLevel in particular should stop being locally owned.
 */
class UserProfileDataStore(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("profile_name")
        val SELECTED_CLASSES = stringSetPreferencesKey("profile_selected_classes")
        val ACCESS_LEVEL = stringPreferencesKey("profile_access_level")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("profile_notifications_enabled")
    }

    val profileFlow: Flow<UserProfile> = context.userProfileDataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.NAME] ?: "",
            selectedClasses = (prefs[Keys.SELECTED_CLASSES] ?: emptySet())
                .mapNotNull { runCatching { UserClass.valueOf(it) }.getOrNull() }.toSet(),
            accessLevel = prefs[Keys.ACCESS_LEVEL]?.let { runCatching { AccessLevel.valueOf(it) }.getOrNull() }
                ?: AccessLevel.MEMBER,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
        )
    }

    suspend fun update(profile: UserProfile) {
        context.userProfileDataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name
            prefs[Keys.SELECTED_CLASSES] = profile.selectedClasses.map { it.name }.toSet()
            prefs[Keys.ACCESS_LEVEL] = profile.accessLevel.name
            prefs[Keys.NOTIFICATIONS_ENABLED] = profile.notificationsEnabled
        }
    }
}
