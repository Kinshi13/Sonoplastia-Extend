package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.escalachurch.app.ui.navigation.AppDestination

private data class NavEntry(val destination: AppDestination, val label: String, val icon: ImageVector)

private val navEntries = listOf(
    NavEntry(AppDestination.Home, "Início", Icons.Filled.Home),
    NavEntry(AppDestination.Doxology, "Doxologia", Icons.Filled.MusicNote),
    NavEntry(AppDestination.Program, "Programar", Icons.Filled.EditCalendar),
    NavEntry(AppDestination.Calendar, "Calendário", Icons.Filled.CalendarMonth),
    NavEntry(AppDestination.Settings, "Ajustes", Icons.Filled.Settings)
)

@Composable
fun EscalaBottomNavBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = androidx.compose.ui.Modifier.height(76.dp)
    ) {
        navEntries.forEach { entry ->
            NavigationBarItem(
                selected = currentDestination == entry.destination,
                onClick = { onNavigate(entry.destination) },
                icon = { Icon(entry.icon, contentDescription = entry.label) },
                label = { Text(entry.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
