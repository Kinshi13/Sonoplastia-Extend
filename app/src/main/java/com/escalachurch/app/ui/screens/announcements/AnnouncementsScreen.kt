package com.escalachurch.app.ui.screens.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.ui.components.AnnouncementCard
import com.escalachurch.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    onBack: () -> Unit,
    onOpenCalendarDate: (java.time.LocalDate) -> Unit
) {
    val viewModel = appViewModel { container ->
        AnnouncementViewModel(container.announcementRepository, container.settingsRepository, container.userProfileRepository, container.adminSession)
    }
    val state by viewModel.uiState.collectAsState()
    var editingTarget by remember { mutableStateOf<AnnouncementEditTarget?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.markAllSeen() }

    if (isEditing) {
        AnnouncementEditScreen(
            existing = editingTarget?.item,
            isAdmin = state.isAdmin,
            onSave = { viewModel.save(it); isEditing = false },
            onDelete = editingTarget?.item?.let { item -> { viewModel.delete(item); isEditing = false } },
            onBack = { isEditing = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anúncios") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        },
        floatingActionButton = {
            if (state.isAdmin) {
                FloatingActionButton(
                    onClick = { editingTarget = AnnouncementEditTarget(null); isEditing = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Filled.Add, contentDescription = "Novo anúncio") }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.announcements.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Campaign,
                    title = "Nenhum anúncio publicado ainda.",
                    message = "Quando houver novidades da igreja, elas aparecerão aqui."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    items(state.announcements, key = { it.id }) { announcement: Announcement ->
                        val isNew = announcement.publishedAt > state.lastSeenAt
                        val highlighted = announcement.affectedClasses.any { it in state.myClasses }
                        AnnouncementCard(
                            announcement = announcement,
                            isNew = isNew,
                            highlighted = highlighted,
                            onOpenCalendar = announcement.relatedEventDate?.let { date -> { onOpenCalendarDate(date) } },
                            onClick = if (state.isAdmin) {
                                { editingTarget = AnnouncementEditTarget(announcement); isEditing = true }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

private data class AnnouncementEditTarget(val item: Announcement?)
