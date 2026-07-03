package com.escalachurch.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.escalachurch.app.ui.components.EscalaBottomNavBar
import com.escalachurch.app.ui.screens.announcements.AnnouncementsScreen
import com.escalachurch.app.ui.screens.calendar.CalendarScreen
import com.escalachurch.app.ui.screens.doxology.DoxologyScreen
import com.escalachurch.app.ui.screens.generalscale.GeneralScaleScreen
import com.escalachurch.app.ui.screens.home.HomeScreen
import com.escalachurch.app.ui.screens.program.ProgramScreen
import com.escalachurch.app.ui.screens.settings.SettingsScreen

@Composable
fun EscalaChurchNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentDestination = when (currentRoute) {
        AppDestination.Doxology.route -> AppDestination.Doxology
        AppDestination.Program.route -> AppDestination.Program
        AppDestination.Calendar.route -> AppDestination.Calendar
        AppDestination.Announcements.route -> AppDestination.Announcements
        AppDestination.Settings.route -> AppDestination.Settings
        else -> AppDestination.Home
    }

    val isSecondaryScreen = currentRoute == SecondaryDestination.GENERAL_SCALE_ROUTE

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (!isSecondaryScreen) {
                EscalaBottomNavBar(
                    currentDestination = currentDestination,
                    onNavigate = { destination -> navigateToTab(destination.route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    onOpenGeneralScale = { date -> navController.navigate(SecondaryDestination.generalScaleRoute(date)) },
                    onOpenAnnouncements = { navigateToTab(AppDestination.Announcements.route) }
                )
            }
            composable(AppDestination.Doxology.route) { DoxologyScreen() }
            composable(AppDestination.Program.route) { ProgramScreen() }
            composable(AppDestination.Calendar.route) { CalendarScreen() }
            composable(AppDestination.Announcements.route) {
                AnnouncementsScreen(
                    onOpenCalendarDate = { navigateToTab(AppDestination.Calendar.route) }
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    onOpenGeneralScale = { navController.navigate(SecondaryDestination.generalScaleRoute(null)) },
                    onOpenAnnouncements = { navigateToTab(AppDestination.Announcements.route) }
                )
            }
            composable(
                SecondaryDestination.GENERAL_SCALE_ROUTE,
                arguments = listOf(navArgument("date") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val dateArg = entry.arguments?.getString("date").orEmpty()
                GeneralScaleScreen(
                    initialDate = dateArg.takeIf { it.isNotBlank() }?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
