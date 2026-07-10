package com.escalachurch.app.ui.stellacore

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.escalachurch.app.di.AppContainer
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.AccessLevel
import com.escalachurch.app.entitlements.FeatureKey
import com.escalachurch.app.ui.navigation.AppDestination
import com.escalachurch.app.ui.navigation.SecondaryDestination

/**
 * Turns "what screen is showing right now" into the small ordered action list Stella Core fans
 * out - the only place in the app that knows both the current route AND how to reach every
 * screen's own create/duplicate/filter capability (via [StellaCoreBus], since most of those
 * capabilities are local composable state the owning screen never exposed as a route or a
 * ViewModel the rest of the app can call). New screens add themselves here, not inside
 * [StellaCore]/[StellaCoreMenu], which stay screen-agnostic.
 *
 * A handful of actions from the Fase 5 spec (duplicar/reordenar itens específicos, filtrar
 * anúncios por classe, seções ancoradas em Configurações) aren't included yet - they need state
 * the owning screen doesn't currently hoist (which item is selected, a scroll anchor). Left out
 * rather than wired to a no-op, per "não implementar pela metade".
 */
@Composable
fun rememberStellaCoreActions(
    currentRoute: String?,
    onNavigateTab: (AppDestination) -> Unit,
    onNavigateRoute: (String) -> Unit
): List<ResolvedStellaCoreAction> {
    val container = rememberAppContainer()
    val isAdmin by container.adminSession.isUnlocked.collectAsState()
    val role = if (isAdmin) AccessLevel.ADMIN else AccessLevel.MEMBER

    val rawActions = remember(currentRoute, isAdmin) {
        actionsForRoute(currentRoute, role, onNavigateTab, onNavigateRoute)
    }
    val roleFiltered = StellaCorePermissionResolver.resolve(rawActions, role)
    return StellaCoreFeatureResolver.resolve(roleFiltered, container.entitlementService)
}

private fun actionsForRoute(
    route: String?,
    role: AccessLevel,
    onNavigateTab: (AppDestination) -> Unit,
    onNavigateRoute: (String) -> Unit
): List<StellaCoreAction> = when {
    route == AppDestination.Home.route -> listOf(
        StellaCoreAction("home_new_personal", "Nova escala pessoal", Icons.AutoMirrored.Filled.EventNote) {
            onNavigateTab(AppDestination.Program); StellaCoreBus.send(StellaCoreCommand.NewPersonalEvent)
        },
        StellaCoreAction("home_general_scale", "Escala Geral", Icons.Filled.CalendarViewDay) {
            onNavigateRoute(SecondaryDestination.generalScaleRoute(null))
        },
        StellaCoreAction("home_next_program", "Próxima programação", Icons.Filled.MusicNote) {
            onNavigateTab(AppDestination.Doxology)
        },
        StellaCoreAction("home_new_event", "Novo evento", Icons.Filled.Add) {
            onNavigateTab(AppDestination.Calendar); StellaCoreBus.send(StellaCoreCommand.GoToTodayCalendar)
        }
    )

    route == SecondaryDestination.GENERAL_SCALE_ROUTE -> listOf(
        StellaCoreAction("scale_new", "Nova escala", Icons.Filled.Add, requiresRole = AccessLevel.ADMIN) {
            StellaCoreBus.send(StellaCoreCommand.NewOfficialScale)
        },
        StellaCoreAction("scale_filter", "Filtrar", Icons.Filled.FilterList) {
            StellaCoreBus.send(StellaCoreCommand.ToggleOnlyMyClassesGeneralScale)
        },
        StellaCoreAction("scale_today", "Ir para hoje", Icons.Filled.Today) {
            StellaCoreBus.send(StellaCoreCommand.GoToTodayGeneralScale)
        }
    )

    route == AppDestination.Doxology.route -> listOfNotNull(
        if (role == AccessLevel.ADMIN) {
            StellaCoreAction("doxology_new", "Nova Doxologia", Icons.Filled.Add, requiresRole = AccessLevel.ADMIN) {
                StellaCoreBus.send(StellaCoreCommand.NewDoxology)
            }
        } else null
    )

    route == AppDestination.Calendar.route -> listOf(
        StellaCoreAction("calendar_new_personal", "Nova escala pessoal", Icons.AutoMirrored.Filled.EventNote) {
            onNavigateTab(AppDestination.Program); StellaCoreBus.send(StellaCoreCommand.NewPersonalEvent)
        },
        StellaCoreAction("calendar_today", "Ir para hoje", Icons.Filled.Today) {
            StellaCoreBus.send(StellaCoreCommand.GoToTodayCalendar)
        }
    )

    route == AppDestination.Announcements.route -> buildList {
        if (role == AccessLevel.ADMIN) {
            add(StellaCoreAction("announcements_new", "Novo anúncio", Icons.Filled.Add, requiresRole = AccessLevel.ADMIN) {
                StellaCoreBus.send(StellaCoreCommand.NewAnnouncement)
            })
            add(StellaCoreAction("announcements_media", "Gerenciar mídia", Icons.Filled.FolderOpen,
                requiresRole = AccessLevel.ADMIN, requiresFeature = FeatureKey.ADVANCED_MEDIA, lockedPreview = true) {
                onNavigateRoute(SecondaryDestination.SONOPLASTIA_ROUTE)
            })
        }
        add(StellaCoreAction("announcements_calendar", "Próximos eventos", Icons.Filled.CalendarMonth) {
            onNavigateTab(AppDestination.Calendar)
        })
    }

    route == SecondaryDestination.SETTINGS_ROUTE -> listOf(
        StellaCoreAction("settings_plans", "Plano atual", Icons.Filled.Sell) {
            onNavigateRoute(SecondaryDestination.PLANS_ROUTE)
        },
        StellaCoreAction("settings_announcements", "Anúncios", Icons.Filled.Campaign) {
            onNavigateTab(AppDestination.Announcements)
        }
    )

    else -> emptyList()
}
