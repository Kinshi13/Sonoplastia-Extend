package com.escalachurch.app.domain.util

import java.time.LocalDateTime

/**
 * Centralizes the "what should I show right now" rule used by both the Home
 * (scales) and Doxology screens: among all items ordered by date+time, find
 * the closest one that has not started yet; if everything is in the past,
 * fall back to the very last (most recent past) item so navigation still has
 * something to show, and let the caller decide when there is nothing at all.
 */
object NextItemResolver {

    /** Returns the index of the next upcoming item, or the last item if all are past, or null if empty. */
    fun <T> resolveStartIndex(items: List<T>, dateTimeOf: (T) -> LocalDateTime, now: LocalDateTime): Int? {
        if (items.isEmpty()) return null
        val sorted = items.withIndex().sortedBy { dateTimeOf(it.value) }
        val upcoming = sorted.firstOrNull { !dateTimeOf(it.value).isBefore(now) }
        return (upcoming ?: sorted.last()).index
    }

    /** Sorts items chronologically (ascending) by their date/time. */
    fun <T> sortedByDateTime(items: List<T>, dateTimeOf: (T) -> LocalDateTime): List<T> =
        items.sortedBy { dateTimeOf(it) }

    fun <T> hasFutureItem(items: List<T>, dateTimeOf: (T) -> LocalDateTime, now: LocalDateTime): Boolean =
        items.any { !dateTimeOf(it).isBefore(now) }
}
