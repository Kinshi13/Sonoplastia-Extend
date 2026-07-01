package com.escalachurch.app.ui.navigation

/** Top-level destinations reachable from the bottom navigation bar. */
sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Doxology : AppDestination("doxology")
    data object Program : AppDestination("program")
    data object Calendar : AppDestination("calendar")
    data object Settings : AppDestination("settings")
}
