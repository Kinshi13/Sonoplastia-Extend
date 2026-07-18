package com.escalachurch.app.share

import android.util.Log

/**
 * Fase 11.10 - `church_access_shared` event. There is no analytics SDK/backend anywhere in this
 * app today (checked the whole codebase - no Firebase, no custom event table, nothing to
 * "reutilizar"), and adding one (Firebase Analytics + google-services.json, or a new backend
 * table) is a bigger decision than this feature - not done here. This logs to Logcat only, with
 * exactly the fields the phase asked for, so swapping in a real destination later only means
 * changing [logShareEvent]'s body, not any call site.
 *
 * Never logs a recipient (Bloco: "não registrar destinatário") - there IS no recipient parameter.
 */
object ChurchAccessAnalytics {
    private const val EVENT_NAME = "church_access_shared"

    fun logShareEvent(churchSlug: String, shareType: ShareChurchAccessAction, platform: String = "android") {
        val timestamp = System.currentTimeMillis()
        Log.i(
            EVENT_NAME,
            "church_slug=$churchSlug share_type=${shareType.analyticsLabel} platform=$platform timestamp=$timestamp"
        )
    }
}
