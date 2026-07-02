package com.escalachurch.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unified, read-only view of anything that can appear on the calendar
 * (scale, doxology, custom event or holiday), used for sorting/marking days
 * without each screen needing to know about every concrete type.
 */
sealed class AgendaEntry {
    abstract val date: LocalDate
    abstract val startTime: LocalTime
    abstract val title: String

    val dateTime: LocalDateTime get() = LocalDateTime.of(date, startTime)

    data class Scale(val scale: ScaleItem) : AgendaEntry() {
        override val date get() = scale.date
        override val startTime get() = scale.startTime
        override val title get() = scale.title
    }

    data class Doxology(val doxology: DoxologyItem) : AgendaEntry() {
        override val date get() = doxology.date
        override val startTime get() = doxology.startTime
        override val title get() = doxology.title
    }

    data class Event(val event: CustomEvent) : AgendaEntry() {
        override val date get() = event.date
        override val startTime get() = event.startTime
        override val title get() = event.title
    }

    data class Holiday(val holiday: com.escalachurch.app.domain.holidays.Holiday) : AgendaEntry() {
        override val date get() = holiday.date
        override val startTime: LocalTime = LocalTime.MIDNIGHT
        override val title get() = holiday.name
    }

    data class AnnouncementEntry(val announcement: Announcement) : AgendaEntry() {
        override val date get() = announcement.relatedEventDate ?: LocalDate.MIN
        override val startTime: LocalTime = LocalTime.MIDNIGHT
        override val title get() = announcement.title
    }
}
