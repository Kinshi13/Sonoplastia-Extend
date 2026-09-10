package com.escalachurch.app.ui.navigation

/** Top-level destinations reachable from the bottom navigation bar. */
sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Doxology : AppDestination("doxology")
    data object Program : AppDestination("program")
    data object Calendar : AppDestination("calendar")
    data object Announcements : AppDestination("announcements")
}

/**
 * Secondary destination, deliberately kept out of the bottom navigation bar (per design
 * guidance: don't overcrowd it) - reached instead via icons on Início/Anúncios.
 */
object SecondaryDestination {
    const val GENERAL_SCALE_ROUTE = "general_scale?date={date}"
    const val SONOPLASTIA_ROUTE = "sonoplastia"
    const val SETTINGS_ROUTE = "settings"
    const val BULLETINS_ROUTE = "bulletins"
    const val PLANS_ROUTE = "plans"
    const val WORSHIP_ROUTE = "worship"
    const val SCALE_IMPORT_ROUTE = "scale_import"

    fun generalScaleRoute(date: java.time.LocalDate?): String =
        "general_scale?date=${date?.toString() ?: ""}"
}
