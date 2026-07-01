package com.escalachurch.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.escalachurch.app.ui.components.EscalaBottomNavBar
import com.escalachurch.app.ui.screens.calendar.CalendarScreen
import com.escalachurch.app.ui.screens.doxology.DoxologyScreen
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
        AppDestination.Settings.route -> AppDestination.Settings
        else -> AppDestination.Home
    }

    Scaffold(
        bottomBar = {
            EscalaBottomNavBar(
                currentDestination = currentDestination,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppDestination.Home.route) { HomeScreen() }
            composable(AppDestination.Doxology.route) { DoxologyScreen() }
            composable(AppDestination.Program.route) { ProgramScreen() }
            composable(AppDestination.Calendar.route) { CalendarScreen() }
            composable(AppDestination.Settings.route) { SettingsScreen() }
        }
    }
}
