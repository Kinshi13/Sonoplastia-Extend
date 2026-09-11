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

// Fase 11.11 - HOME | ESCALAS | ANÚNCIOS | CALENDÁRIO (same left-to-right order as the bottom nav
// bar's navEntries, minus the star - it isn't a route). Used to decide which way a tab-to-tab
// transition should slide, so it always matches the swipe direction. Escalas is compared by its
// route *template* (SecondaryDestination.GENERAL_SCALE_ROUTE), not a filled-in value - that's what
// NavBackStackEntry.destination.route actually reports for a parameterized destination.
private val tabOrder = listOf(
    AppDestination.Home.route,
    SecondaryDestination.GENERAL_SCALE_ROUTE,
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
 * Fase 11.9A hotfix - the previous version of this file gated with a plain top-level `when`
 * outside any NavHost at all. That should have been equivalent, but there was no real, inspectable
 * "current route" to prove it (the earlier hotfix report couldn't confirm the actual startDestination
 * in effect). This version makes it literal: a real NavHost whose startDestination is
 * [Routes.BOOTSTRAP], never Home - Home/the rest of the app only exists behind [Routes.APP], which
 * [BootstrapState.HasActiveChurch] is the only thing that ever navigates to. See [routeFor] for the
 * (now independently unit-testable) BootstrapState -> route mapping.
 */
private object Routes {
    const val BOOTSTRAP = "bootstrap"
    const val CHURCH_ENTRY = "church_entry"
    const val APP = "app"
}

/** Pure - which route a given [BootstrapState] should land the user on. No Android/Compose
 *  dependency, so this is directly unit-tested (see NavGraphRouteTest) instead of only provable
 *  by running the real app. There is deliberately no `else` branch: every state maps explicitly,
 *  so a new BootstrapState case that's forgotten here fails to compile instead of silently
 *  falling through to APP/Home. */
internal fun routeFor(state: BootstrapState): String = when (state) {
    is BootstrapState.Loading -> Routes.BOOTSTRAP
    is BootstrapState.Error -> Routes.CHURCH_ENTRY
    is BootstrapState.NeedsChurchEntry -> Routes.CHURCH_ENTRY
    is BootstrapState.HasActiveChurch -> Routes.APP
}

@Composable
fun EscalaChurchNavGraph() {
    val container = rememberAppContainer()
    val bootstrapState by container.activeChurchManager.bootstrapState.collectAsState()
    val navController = rememberNavController()

    if (com.escalachurch.app.BuildConfig.DEBUG) {
        android.util.Log.d("ChurchBootstrap", "NavHostStartDestination=${Routes.BOOTSTRAP}")
    }

    LaunchedEffect(bootstrapState) {
        val target = routeFor(bootstrapState)
        val current = navController.currentDestination?.route
        if (com.escalachurch.app.BuildConfig.DEBUG) {
            android.util.Log.d("ChurchBootstrap", "NavigationTarget target=$target currentRoute=$current")
        }
        if (target != Routes.BOOTSTRAP && current != target) {
            // Fully resets the back stack to the new target - this is a top-level auth/onboarding
            // gate switch (fresh install <-> has a church <-> switched church), never a screen the
            // user should be able to back-swipe out of into the wrong state.
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.BOOTSTRAP) {
        composable(Routes.BOOTSTRAP) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable(Routes.CHURCH_ENTRY) { ChurchEntryScreen() }
        composable(Routes.APP) { EscalaChurchAppNavGraph() }
    }
}

@Composable
private fun EscalaChurchAppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Fase 11.11 - the bottom bar/Stella Core now key off the raw route string (currentRoute)
    // instead of an AppDestination, since Escalas (GENERAL_SCALE_ROUTE) is a bottom-bar tab now
    // but was never an AppDestination case - see BottomNavBar.kt's NavEntry.matchRoute.

    // Fase 11.11 - Escalas removed from this list: it now keeps the bottom bar visible and reads
    // as a true tab (GeneralScaleScreen's own internal chrome/back arrow is untouched either way).
    val isSecondaryScreen = currentRoute == SecondaryDestination.SONOPLASTIA_ROUTE ||
        currentRoute == SecondaryDestination.SETTINGS_ROUTE ||
        currentRoute == SecondaryDestination.BULLETINS_ROUTE ||
        currentRoute == SecondaryDestination.PLANS_ROUTE ||
        currentRoute == SecondaryDestination.WORSHIP_ROUTE ||
        currentRoute == SecondaryDestination.SCALE_IMPORT_ROUTE

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
    // Fase 11.9B Bloco 7/18 - system Back must close the Core first, never leave the screen while
    // it's open (a real gap before this: Back fell straight through to normal navigation).
    androidx.activity.compose.BackHandler(enabled = stellaOpen) { stellaOpen = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!isSecondaryScreen) {
                    EscalaBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route -> navigateToTab(route) },
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
                    onOpenPlans = { navController.navigate(SecondaryDestination.PLANS_ROUTE) },
                    onOpenAnnouncements = { navigateToTab(AppDestination.Announcements.route) },
                    onOpenWorship = { navController.navigate(SecondaryDestination.WORSHIP_ROUTE) },
                    onOpenDoxology = { navigateToTab(AppDestination.Doxology.route) },
                    onOpenScaleImport = { navController.navigate(SecondaryDestination.SCALE_IMPORT_ROUTE) }
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
                // Fase 11.11 - Escalas is a bottom-bar tab now (see isSecondaryScreen above): no
                // enter/exit override here, so it inherits the NavHost's tabEnter/tabExit default
                // instead of reading as a screen pushed on top of another.
                SecondaryDestination.GENERAL_SCALE_ROUTE,
                arguments = listOf(navArgument("date") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val dateArg = entry.arguments?.getString("date").orEmpty()
                GeneralScaleScreen(
                    initialDate = dateArg.takeIf { it.isNotBlank() }?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                    onBack = { navController.popBackStack() },
                    onOpenPlans = { navController.navigate(SecondaryDestination.PLANS_ROUTE) }
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
            composable(
                SecondaryDestination.WORSHIP_ROUTE,
                enterTransition = { pushEnter() },
                exitTransition = { pushExit() },
                popExitTransition = { popExitToRight() }
            ) {
                com.escalachurch.app.ui.screens.worship.WorshipScreen(onBack = { navController.popBackStack() })
            }
            composable(
                SecondaryDestination.SCALE_IMPORT_ROUTE,
                enterTransition = { pushEnter() },
                exitTransition = { pushExit() },
                popExitTransition = { popExitToRight() }
            ) {
                com.escalachurch.app.ui.screens.scaleimport.ScaleImportScreen(onBack = { navController.popBackStack() })
            }
        }
        }

        if (isSecondaryScreen) {
            // No bottom bar here to embed a star into - fall back to the floating standalone
            // variant (still a pure single-tap open/close, same as the bar).
            StellaCore(
                actions = stellaCoreActions,
                reducedMotion = !appSettings.animationsEnabled,
                onLockedActionClick = { action -> lockedActionPreview = action },
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
