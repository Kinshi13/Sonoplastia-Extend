package com.escalachurch.app.data.repository

import android.content.Context
import com.escalachurch.app.domain.model.ChangeLogEntityType
import com.escalachurch.app.domain.model.ChangeLogEntry
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.domain.util.ChangeDetector
import com.escalachurch.app.notification.ChangeNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * "Escala Geral": the admin-facing view over official [ScaleItem]s. Wraps [ScaleRepository]
 * (same table as the rest of the app reads from - see HomeViewModel) rather than duplicating
 * storage, and additionally writes a [ChangeLogEntry] + fires a local notification whenever an
 * admin edit affects a class-relevant field, so members find out something changed.
 *
 * TODO(backend): once connected, admin writes here should go to Firestore/Supabase instead of
 * (or in addition to) Room, and the change-detection below should move server-side so it also
 * covers edits made by other admins on other devices.
 */
class GeneralScaleRepository(
    private val context: Context,
    private val scaleRepository: ScaleRepository,
    private val changeLogRepository: ChangeLogRepository,
    private val settingsRepository: SettingsRepository
) {

    fun observeOfficial(): Flow<List<ScaleItem>> =
        scaleRepository.observeAll().map { list -> list.filter { it.sourceType == SourceType.OFFICIAL } }

    /** Persists an official scale (admin-only, enforced by the calling screen), logging/notifying affected classes. */
    suspend fun saveOfficial(item: ScaleItem) {
        val before = if (item.id != 0L) scaleRepository.getById(item.id) else null
        val toSave = item.copy(sourceType = SourceType.OFFICIAL)
        scaleRepository.save(toSave)

        val affected = ChangeDetector.affectedClasses(before, toSave)
        if (affected.isEmpty()) return

        val entry = ChangeLogEntry(
            entityType = ChangeLogEntityType.SCALE,
            entityId = toSave.id,
            affectedClasses = affected,
            title = "Nova alteração na sua escala",
            message = ChangeDetector.summaryMessage(toSave, affected),
            relatedDateIso = toSave.date.toString()
        )
        val id = changeLogRepository.record(entry)

        val settings = settingsRepository.settingsFlow.first()
        if (settings.changeNotificationsEnabled) {
            ChangeNotifier.ensureChannel(context)
            ChangeNotifier.notifyChange(context, entry.copy(id = id))
        }
    }

    suspend fun deleteOfficial(item: ScaleItem) = scaleRepository.delete(item)

    /** Duplicates an official scale to another date (e.g. "duplicar para outro dia/mês"), keeping roles/notes. */
    suspend fun duplicateTo(source: ScaleItem, newDate: java.time.LocalDate) {
        saveOfficial(source.copy(id = 0L, date = newDate, createdAt = System.currentTimeMillis()))
    }

    /** Creates several consecutive/extra official days (e.g. Semana de Oração) from one template. */
    suspend fun createExtraDays(template: ScaleItem, dates: List<java.time.LocalDate>) {
        dates.forEach { date ->
            saveOfficial(template.copy(id = 0L, date = date, createdAt = System.currentTimeMillis()))
        }
    }
}
