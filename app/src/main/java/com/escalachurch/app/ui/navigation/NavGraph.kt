package com.escalachurch.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.escalachurch.app.church.BootstrapState
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.ui.components.EscalaBottomNavBar
import com.escalachurch.app.ui.components.PremiumPreviewSheet
import com.escalachurch.app.ui.screens.announcements.AnnouncementsScreen
import com.escalachurch.app.ui.screens.churchentry.ChurchEntryScreen
import com.escalachurch.app.ui.screens.bulletins.BulletinsScreen
import com.escalachurch.app.ui.screens.calendar.CalendarScreen
import com.escalachurch.app.ui.screens.doxology.DoxologyScreen
import com.escalachurch.app.ui.screens.generalscale.GeneralScaleScreen
import com.escalachurch.app.ui.screens.home.HomeScreen
import com.escalachurch.app.ui.screens.plans.PlansScreen
import com.escalachurch.app.ui.screens.program.ProgramScreen
import com.escalachurch.app.ui.screens.settings.SettingsScreen
import com.escalachurch.app.ui.screens.sonoplastia.SonoplastiaScreen
import com.escalachurch.app.ui.stellacore.StellaCore
import com.escalachurch.app.ui.stellacore.StellaCoreAction
import com.escalachurch.app.ui.stellacore.StellaCoreMenu
import com.escalachurch.app.ui.stellacore.rememberStellaCoreActions

// Same left-to-right order as the bottom nav bar (see BottomNavBar.kt's navEntries) - used to
// decide which way a tab-to-tab transition should slide, so it always matches the swipe direction.
private val tabOrder = listOf(
    AppDestination.Program.route,
    AppDestination.Doxology.route,
    AppDestination.Home.route,
    AppDestination.Announcements.route,
    AppDestination.Calendar.route
)

private fun tabIndexOf(route: String?): Int = tabOrder.indexOf(route).takeIf { it >= 0 } ?: tabOrder.indexOf(AppDestination.Home.route)

private const val TAB_TRANSITION_MS = 280
private val TabEasing = FastOutSlowInEasing

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter() =
    slideInHorizontally(tween(TAB_TRANSITION_MS, easing = TabEasing)) { fullWidth ->
        val dir = if (tabIndexOf(targetState.destination.route) > tabIndexOf(initialState.destination.route)) 1 else -1
        dir * fullWidth / 4
    } + fadeIn(tween(TAB_TRANSITION_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit() =
    slideOutHorizontally(tween(TAB_TRANSITION_MS, easing = TabEasing)) { fullWidth ->
        val dir = if (tabIndexOf(targetState.destination.route) > tabIndexOf(initialState.destination.route)) 1 else -1
        -dir * fullWidth / 4
    } + fadeOut(tween(TAB_TRANSITION_MS))

// Secondary/detail screens (opened from within a tab, not from the bottom bar) read as a "push"
// instead of a tab swap: they slide fully in from the right and back out to the right on close.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnter() =
    slideInHorizontally(tween(TAB_TRANSITION_MS, easing = TabEasing)) { fullWidth -> fullWidth } + fadeIn(tween(TAB_TRANSITION_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushExit() =
    fadeOut(tween(TAB_TRANSITION_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitToRight() =
    slideOutHorizontally(tween(TAB_TRANSITION_MS, easing = TabEasing)) { fullWidth -> fullWidth } + fadeOut(tween(TAB_TRANSITION_MS))

/**
 * Fase 11.9A - gates the whole app behind [BootstrapState] (see ActiveChurchManager). Priority is
 * explicit: Loading first, then an actual blocking Error, then "no church yet" -> ChurchEntry,
 * only then the real app - there is no fixed Home start destination anymore. This also fixes the
 * original hotfix bug: BootstrapState.NeedsChurchEntry only ever comes from ActiveChurchManager
 * actually failing to find evidence of a church (persisted or legacy-migrated), never from a
 * fixed/implicit "ready" flag that could be true before a real decision was made.
 */
@Composable
fun EscalaChurchNavGraph() {
    val container = rememberAppContainer()
    val bootstrapState by container.activeChurchManager.bootstrapState.collectAsState()

    when (bootstrapState) {
        is BootstrapState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is BootstrapState.Error -> {
            // Fase 11.9A only requires this state to exist and take priority over Home - a full
            // retry/error UI is out of scope for this hotfix; ChurchEntryScreen (reachable this
            // way too) already lets the user retry by entering a code.
            ChurchEntryScreen()
        }
        is BootstrapState.NeedsChurchEntry -> {
            ChurchEntryScreen()
        }
        is BootstrapState.HasActiveChurch -> {
            EscalaChurchAppNavGraph()
        }
    }
}

@Composable
private fun EscalaChurchAppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentDestination = when (currentRoute) {
        AppDestination.Doxology.route -> AppDestination.Doxology
        AppDestination.Program.route -> AppDestination.Program
        AppDestination.Calendar.route -> AppDestination.Calendar
        AppDestination.Announcements.route -> AppDestination.Announcements
        else -> AppDestination.Home
    }

    val isSecondaryScreen = currentRoute == SecondaryDestination.GENERAL_SCALE_ROUTE ||
        currentRoute == SecondaryDestination.SONOPLASTIA_ROUTE ||
        currentRoute == SecondaryDestination.SETTINGS_ROUTE ||
        currentRoute == SecondaryDestination.BULLETINS_ROUTE ||
        currentRoute == SecondaryDestination.PLANS_ROUTE

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val appSettings by rememberAppContainer().settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
    val stellaCoreActions = rememberStellaCoreActions(
        currentRoute = currentRoute,
        onNavigateTab = { destination -> navigateToTab(destination.route) },
        onNavigateRoute = { route -> navController.navigate(route) }
    )
    var lockedActionPreview by remember { mutableStateOf<StellaCoreAction?>(null) }
    var stellaOpen by remember { mutableStateOf(false) }
    // Stella Core visually replaces Início in the bar (see BottomNavBar.kt) - closing it whenever
    // the route changes underneath it keeps a stale open fan from lingering after navigation.
    LaunchedEffect(currentRoute) { stellaOpen = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!isSecondaryScreen) {
                    EscalaBottomNavBar(
                        currentDestination = currentDestination,
                        onNavigate = { destination -> navigateToTab(destination.route) },
                        stellaOpen = stellaOpen,
                        onStellaOpenChange = { stellaOpen = it }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
                modifier = Modifier.padding(padding),
                enterTransition = { tabEnter() },
                exitTransition = { tabExit() },
                popEnterTransition = { tabEnter() },
                popExitTransition = { tabExit() }
            ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    onOpenGeneralScale = { date -> navController.navigate(SecondaryDestination.generalScaleRoute(date)) },
                    onOpenSettings = { navController.navigate(SecondaryDestination.SETTINGS_ROUTE) },
                    onOpenBulletins = { navController.navigate(SecondaryDestination.BULLETINS_ROUTE) },
                    onOpenSonoplastia = { navController.navigate(SecondaryDestination.SONOPLASTIA_ROUTE) },
                    onOpenPlans = { navController.navigate(SecondaryDestination.PLANS_ROUTE) }
                )
            }
            composable(AppDestination.Doxology.route) { DoxologyScreen() }
            composable(AppDestination.Program.route) { ProgramScreen() }
            composable(AppDestination.Calendar.route) { CalendarScreen() }
            composable(AppDestination.Announcements.route) {
                AnnouncementsScreen(
                    onOpenCalendarDate = { navigateToTab(AppDestination.Calendar.route) },
                    onOpenBulletins = { navController.navigate(SecondaryDestination.BULLETINS_ROUTE) }
                )
            }
            composable(
                SecondaryDestination.GENERAL_SCALE_ROUTE,
                arguments = listOf(navArgument("date") { type = NavType.StringType; defaultValue = "" }),
                enterTransition = { pushEnter() },
                exitTransition = { pushExit() },
                popExitTransition = { popExitToRight() }
            ) { entry ->
                val dateArg = entry.arguments?.getString("date").orEmpty()
                GeneralScaleScreen(
                    initialDate = dateArg.takeIf { it.isNotBlank() }?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                SecondaryDestination.SONOPLASTIA_ROUTE,
                enterTransition = { pushEnter() },
                exitTransition = { pushExit() },
                popExitTransition = { popExitToRight() }
            ) {
                SonoplastiaScreen(onBack = { navController.popBackStack() })
            }
            composable(
                SecondaryDestination.SETTINGS_ROUTE,
                enterTransition = { pushEnter() },
                exitTransition = { pushExit() },
                popExitTransition = { popExitToRight() }
            ) {
                SettingsScreen(
                    onOpenGeneralScale = { navController.navigate(SecondaryDestination.generalScaleRoute(null)) },
                    onOpenAnnouncements = { navigateToTab(AppDestination.Announcements.route) },
                    onOpenPlans = { navController.navigate(SecondaryDestination.PLANS_ROUTE) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                SecondaryDestination.BULLETINS_ROUTE,
                enterTransition = { pushEnter() },
                exitTransition = { pushExit() },
                popExitTransition = { popExitToRight() }
            ) {
                BulletinsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                SecondaryDestination.PLANS_ROUTE,
                enterTransition = { pushEnter() },
                exitTransition = { pushExit() },
                popExitTransition = { popExitToRight() }
            ) {
                PlansScreen(onBack = { navController.popBackStack() })
            }
        }
        }

        if (isSecondaryScreen) {
            // No bottom bar here to embed a star into - fall back to the floating standalone
            // variant (still single-tap-to-open / double-tap-to-Início, same as the bar).
            StellaCore(
                actions = stellaCoreActions,
                reducedMotion = !appSettings.animationsEnabled,
                onLockedActionClick = { action -> lockedActionPreview = action },
                onNavigateHome = { navigateToTab(AppDestination.Home.route) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else if (stellaOpen && stellaCoreActions.isNotEmpty()) {
            // The star itself lives inside EscalaBottomNavBar (dead center of 5 equal slots, i.e.
            // exactly screen-center) - only the fan needs to be drawn here, anchored just above
            // the 84dp-tall bar so it reads as growing out of the star, not floating separately.
            StellaCoreMenu(
                actions = stellaCoreActions,
                reducedMotion = !appSettings.animationsEnabled,
                onActionSelected = { resolved ->
                    stellaOpen = false
                    if (resolved.isLocked) lockedActionPreview = resolved.action else resolved.action.onClick()
                },
                onDismiss = { stellaOpen = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp)
            )
        }
    }

    lockedActionPreview?.let { action ->
        PremiumPreviewSheet(
            featureName = action.label,
            featureDescription = "Esse recurso faz parte de um plano superior. Veja os planos disponíveis para desbloqueá-lo.",
            onSeePlans = { lockedActionPreview = null; navController.navigate(SecondaryDestination.PLANS_ROUTE) },
            onDismiss = { lockedActionPreview = null }
        )
    }
}
