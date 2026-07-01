package com.escalachurch.app.domain.holidays

import java.time.LocalDate

data class Holiday(
    val name: String,
    val date: LocalDate,
    val isMovable: Boolean = false
)

/**
 * Simple local provider for Brazilian national holidays.
 *
 * Fixed-date holidays are calculated for any year. Movable holidays (Carnaval,
 * Sexta-feira Santa, Corpus Christi) depend on Easter, which is computed with
 * the Anonymous Gregorian algorithm below - no network/API required.
 *
 * The structure is intentionally kept as a plain provider function so it can
 * later be swapped for (or merged with) a remote holidays API without
 * touching any call site: just replace [BrazilianHolidays.forYear] internals.
 */
object BrazilianHolidays {

    fun forYear(year: Int): List<Holiday> {
        val easter = calculateEaster(year)
        return listOf(
            Holiday("Confraternização Universal", LocalDate.of(year, 1, 1)),
            Holiday("Carnaval", easter.minusDays(47), isMovable = true),
            Holiday("Sexta-feira Santa", easter.minusDays(2), isMovable = true),
            Holiday("Tiradentes", LocalDate.of(year, 4, 21)),
            Holiday("Dia do Trabalhador", LocalDate.of(year, 5, 1)),
            Holiday("Independência do Brasil", LocalDate.of(year, 9, 7)),
            Holiday("Nossa Senhora Aparecida", LocalDate.of(year, 10, 12)),
            Holiday("Finados", LocalDate.of(year, 11, 2)),
            Holiday("Proclamação da República", LocalDate.of(year, 11, 15)),
            Holiday("Natal", LocalDate.of(year, 12, 25))
        ).sortedBy { it.date }
    }

    fun between(start: LocalDate, end: LocalDate): List<Holiday> {
        val years = (start.year..end.year).toSet()
        return years.flatMap { forYear(it) }.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
    }

    fun nextUpcoming(from: LocalDate, limit: Int = 3): List<Holiday> {
        val candidates = forYear(from.year) + forYear(from.year + 1)
        return candidates.filter { !it.date.isBefore(from) }.sortedBy { it.date }.take(limit)
    }

    /** Anonymous Gregorian algorithm (Meeus/Jones/Butcher) for the date of Easter Sunday. */
    private fun calculateEaster(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}
