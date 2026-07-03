package com.escalachurch.app.di

import android.content.Context
import com.escalachurch.app.data.local.AppDatabase
import com.escalachurch.app.data.preferences.SettingsDataStore
import com.escalachurch.app.data.preferences.UserProfileDataStore
import com.escalachurch.app.data.repository.AnnouncementRepository
import com.escalachurch.app.data.repository.ChangeLogRepository
import com.escalachurch.app.data.repository.CustomEventRepository
import com.escalachurch.app.data.repository.DoxologyRepository
import com.escalachurch.app.data.repository.GeneralScaleRepository
import com.escalachurch.app.data.repository.ScaleRepository
import com.escalachurch.app.data.repository.SettingsRepository
import com.escalachurch.app.data.repository.UserProfileRepository
import com.escalachurch.app.security.AdminSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Minimal, manual dependency container. No DI framework is required; this keeps the build
 * lighter while still separating construction from usage for testability.
 *
 * Official data (scales, doxologies, announcements) lives in Firebase Firestore/Storage/Auth -
 * see FirestoreCollections - so it's shared in real time by every device signed into the same
 * Firebase project. Personal data (Programar's CustomEvent, local settings, reminder state)
 * stays in Room/DataStore on-device, since it was never meant to be shared.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val userProfileDataStore = UserProfileDataStore(context)

    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance()

    val scaleRepository = ScaleRepository(firestore)
    val doxologyRepository = DoxologyRepository(firestore)
    val customEventRepository = CustomEventRepository(database.customEventDao())
    val settingsRepository = SettingsRepository(settingsDataStore)
    val userProfileRepository = UserProfileRepository(userProfileDataStore)
    val announcementRepository = AnnouncementRepository(firestore, firebaseStorage)
    val changeLogRepository = ChangeLogRepository(database.changeLogDao())

    val generalScaleRepository = GeneralScaleRepository(
        context = context,
        scaleRepository = scaleRepository,
        changeLogRepository = changeLogRepository,
        settingsRepository = settingsRepository
    )

    val adminSession = AdminSession(firebaseAuth, userProfileRepository)
}
