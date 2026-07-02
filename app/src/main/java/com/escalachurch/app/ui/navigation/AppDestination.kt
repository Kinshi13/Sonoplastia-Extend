package com.escalachurch.app.ui.navigation

/** Top-level destinations reachable from the bottom navigation bar. */
sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Doxology : AppDestination("doxology")
    data object Program : AppDestination("program")
    data object Calendar : AppDestination("calendar")
    data object Settings : AppDestination("settings")
}

/**
 * Secondary destinations, deliberately kept out of the bottom navigation bar (per design
 * guidance: don't overcrowd it) - reached instead via icons on Início/Configurações.
 */
object SecondaryDestination {
    const val GENERAL_SCALE_ROUTE = "general_scale?date={date}"
    const val ANNOUNCEMENTS_ROUTE = "announcements"

    fun generalScaleRoute(date: java.time.LocalDate?): String =
        "general_scale?date=${date?.toString() ?: ""}"
}
