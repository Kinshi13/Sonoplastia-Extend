package com.escalachurch.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.escalachurch.app.EscalaChurchApp
import com.escalachurch.app.domain.util.ReminderMatcher
import com.escalachurch.app.ui.components.dayOfWeekLabel
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Periodically checks upcoming scales for a match with the local user's name
 * (set in Configurações) and fires a local notification once per reminder
 * "kind" (day-before / hours-before) per scale, using DataStore to remember
 * what was already sent so nothing repeats.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as EscalaChurchApp).container
        val settings = container.settingsRepository.settingsFlow.first()
        if (!settings.remindersEnabled || settings.myName.isBlank()) return Result.success()

        ReminderNotifier.ensureChannel(applicationContext)

        val scales = container.scaleRepository.observeAll().first()
        val now = LocalDateTime.now()
        val alreadyNotified = container.settingsRepository.notifiedReminderKeysFlow.first()

        scales.forEach { scale ->
            val scaleDateTime = LocalDateTime.of(scale.date, scale.startTime)
            if (scaleDateTime.isBefore(now)) return@forEach

            val match = ReminderMatcher.match(scale, settings.myName) ?: return@forEach
            val hoursUntil = ChronoUnit.HOURS.between(now, scaleDateTime)

            if (settings.notifyDayBefore && hoursUntil in 0..24) {
                fireOnce(scale.id, "DAY_BEFORE", alreadyNotified, match, "Amanhã, ${scale.date.dayOfWeekLabel()}")
            }
            if (settings.notifyHoursBefore && hoursUntil in 0..settings.reminderHoursBeforeLead.toLong()) {
                fireOnce(scale.id, "HOURS_BEFORE", alreadyNotified, match, "Em poucas horas")
            }
        }

        container.settingsRepository.pruneReminderKeys(scales.map { it.id }.toSet())
        return Result.success()
    }

    private suspend fun fireOnce(
        scaleId: String,
        kind: String,
        alreadyNotified: Set<String>,
        match: com.escalachurch.app.domain.util.ScaleMatch,
        leadDescription: String
    ) {
        val key = "$scaleId:$kind"
        if (key in alreadyNotified) return
        val container = (applicationContext as EscalaChurchApp).container
        ReminderNotifier.notifyMatch(applicationContext, match, leadDescription, notificationId = key.hashCode())
        container.settingsRepository.markReminderNotified(key)
    }

    companion object {
        private const val WORK_NAME = "escala_church_reminder_check"

        /** Schedules the recurring check; safe to call on every app start (replaces the existing schedule). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofHours(6))
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
