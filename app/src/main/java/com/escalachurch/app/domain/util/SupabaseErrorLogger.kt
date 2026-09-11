package com.escalachurch.app.domain.util

import android.util.Log
import com.escalachurch.app.BuildConfig
import io.github.jan.supabase.exceptions.RestException

/**
 * Homologação (correção de criação/edição de anúncios) - every save/upload failure used to be
 * silently discarded: [friendlyErrorMessage] maps the exception to a short, honest UI message
 * (correctly, per Bloco 6 - technical Supabase text must never reach the user), but nothing ever
 * logged what the *real* failure actually was, anywhere - not even in Logcat. That made a genuine
 * backend/RLS/payload problem indistinguishable from a flaky network from the developer's side too.
 *
 * This logs the real cause to Logcat, DEBUG builds only, tagged by which step failed
 * (`stage`, e.g. "INSERT_ANNOUNCEMENT") - never in a release build, and never any secret. Never
 * pass this a raw request/response containing headers - only the exception itself.
 */
object SupabaseErrorLogger {

    private const val TAG = "SupabaseError"

    fun log(stage: String, error: Throwable) {
        if (!BuildConfig.DEBUG) return
        val details = when (error) {
            is RestException -> "error=${error.error} description=${error.description}"
            else -> "message=${error.message}"
        }
        Log.e(TAG, "[$stage] ${error::class.simpleName} - $details", error)
    }
}
