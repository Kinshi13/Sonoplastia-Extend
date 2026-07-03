package com.escalachurch.app.ui.screens.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.ui.components.AnnouncementCard
import com.escalachurch.app.ui.components.EmptyState
import com.escalachurch.app.ui.components.ErrorBanner
import com.escalachurch.app.ui.components.PulledUpEntrance
import com.escalachurch.app.ui.components.rememberEntranceVisible

@Composable
fun AnnouncementsScreen(
    onOpenCalendarDate: (java.time.LocalDate) -> Unit
) {
    val viewModel = appViewModel { container ->
        AnnouncementViewModel(container.announcementRepository, container.settingsRepository, container.userProfileRepository, container.adminSession)
    }
    val state by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val appSettings by rememberAppContainer().settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
    val feedVisible = rememberEntranceVisible(appSettings.animationsEnabled)
    var editingTarget by remember { mutableStateOf<AnnouncementEditTarget?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.markAllSeen() }

    if (isEditing) {
        AnnouncementEditScreen(
            existing = editingTarget?.item,
            isAdmin = state.isAdmin,
            onUpload = { uri -> viewModel.uploadMedia(context, uri) },
            onSave = { viewModel.save(it); isEditing = false },
            onDelete = editingTarget?.item?.let { item -> { viewModel.delete(item); isEditing = false } },
            onBack = { isEditing = false }
        )
        return
    }

    Scaffold(
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
            Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp, start = 20.dp, end = 20.dp)) {
                Text("Anúncios", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                errorMessage?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(message = message, onDismiss = { viewModel.dismissError() })
                }
                Spacer(Modifier.height(20.dp))

                if (state.announcements.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Campaign,
                        title = "Nenhum anúncio publicado ainda.",
                        message = "Quando houver novidades da igreja, elas aparecerão aqui.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    PulledUpEntrance(visible = feedVisible, modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 96.dp)
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
    }
}

private data class AnnouncementEditTarget(val item: Announcement?)
