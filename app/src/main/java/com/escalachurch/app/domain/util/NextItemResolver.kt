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

    /**
     * Index of the item currently "in progress" - the first whose [startOf, endOf] window
     * contains `now` - or null if none has an end time or none is happening right now. Lets a day
     * with several back-to-back sessions (Escola Sabatina, Culto Divino, JA...) default to
     * whichever one is actually happening, instead of always jumping to the next start time.
     */
    fun <T> resolveCurrentIndex(items: List<T>, startOf: (T) -> LocalDateTime, endOf: (T) -> LocalDateTime?, now: LocalDateTime): Int? {
        val index = items.indexOfFirst { item ->
            val end = endOf(item) ?: return@indexOfFirst false
            !now.isBefore(startOf(item)) && !now.isAfter(end)
        }
        return index.takeIf { it >= 0 }
    }
}
