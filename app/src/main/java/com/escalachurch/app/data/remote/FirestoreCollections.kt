package com.escalachurch.app.data.remote

/**
 * Firestore collection names, kept in one place so the security rules (configured in the
 * Firebase console) and the app code never drift apart.
 *
 * Suggested rules for this project: `scales`, `doxologies` and `announcements` are readable by
 * anyone (public schedule/announcements view - what the future website reads too), but writable
 * only by authenticated users (the church admin, signed in via Firebase Auth - see AdminSession).
 */
object FirestoreCollections {
    const val SCALES = "scales"
    const val DOXOLOGIES = "doxologies"
    const val ANNOUNCEMENTS = "announcements"
}
