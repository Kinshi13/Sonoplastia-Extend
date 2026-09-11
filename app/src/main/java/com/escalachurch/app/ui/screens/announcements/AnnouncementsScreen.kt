package com.escalachurch.app.ui.screens.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.di.rememberAppContainer
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.share.AnnouncementShareUseCase
import com.escalachurch.app.share.buildAnnouncementShareMessage
import com.escalachurch.app.ui.components.AnnouncementCard
import com.escalachurch.app.ui.components.ConfirmDialog
import com.escalachurch.app.ui.components.EmptyState
import com.escalachurch.app.ui.components.ErrorBanner
import com.escalachurch.app.ui.components.PulledUpEntrance
import com.escalachurch.app.ui.components.ShareChurchAccessViewModel
import com.escalachurch.app.ui.components.rememberEntranceVisible
import kotlinx.coroutines.launch

@Composable
fun AnnouncementsScreen(
    onOpenCalendarDate: (java.time.LocalDate) -> Unit,
    onOpenBulletins: () -> Unit = {}
) {
    val viewModel = appViewModel { container ->
        AnnouncementViewModel(container.announcementRepository, container.settingsRepository, container.userProfileRepository, container.adminSession)
    }
    val state by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val appSettings by rememberAppContainer().settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
    // Android replica of the web's AnnouncementShareButton - reuses the same church-identity
    // resolver as "Compartilhar acesso da igreja" instead of a second church-name/link lookup.
    val shareChurchViewModel = appViewModel { container -> ShareChurchAccessViewModel(container.activeChurchManager) }
    val shareChurchState by shareChurchViewModel.uiState.collectAsState()
    val shareScope = rememberCoroutineScope()
    val feedVisible = rememberEntranceVisible(appSettings.animationsEnabled)
    var editingTarget by remember { mutableStateOf<AnnouncementEditTarget?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Announcement?>(null) }
    // Fase 11.9B Entrega 3 Bloco 3 - "não tocar vários vídeos simultaneamente": one shared id for
    // the whole feed, passed down to every AnnouncementCard (see its isActivePlayer/onRequestPlay).
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.markAllSeen() }

    LaunchedEffect(Unit) {
        com.escalachurch.app.ui.stellacore.StellaCoreBus.events().collect { command ->
            if (command == com.escalachurch.app.ui.stellacore.StellaCoreCommand.NewAnnouncement) {
                editingTarget = AnnouncementEditTarget(null); isEditing = true
            }
        }
    }

    if (isEditing) {
        AnnouncementEditScreen(
            existing = editingTarget?.item,
            isAdmin = state.isAdmin,
            onUpload = { uri -> viewModel.uploadMedia(context, uri) },
            onSave = { item ->
                val isNewAnnouncement = editingTarget?.item == null
                val result = viewModel.save(item)
                if (result.isSuccess) {
                    val message = if (isNewAnnouncement) "Anúncio publicado" else "Anúncio atualizado"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    null
                } else {
                    // The real cause is already logged (SupabaseErrorLogger, DEBUG builds only) -
                    // this is the same short, honest message the ViewModel already computed via
                    // friendlyErrorMessage(), not a second, unrelated hardcoded string.
                    viewModel.errorMessage.value ?: "Não foi possível salvar. Verifique sua conexão e tente novamente."
                }
            },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Anúncios", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = onOpenBulletins) {
                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Boletins")
                    }
                }
                errorMessage?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(message = message, onDismiss = { viewModel.dismissError() })
                }
                Spacer(Modifier.height(20.dp))

                if (state.isLoading) {
                    com.escalachurch.app.ui.components.CelestialLoadingState(modifier = Modifier.weight(1f))
                } else if (state.hasLoadError) {
                    com.escalachurch.app.ui.components.CelestialOfflineState(onRetry = viewModel::retry, modifier = Modifier.weight(1f))
                } else if (state.announcements.isEmpty()) {
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
                                    onEdit = if (state.isAdmin) {
                                        { editingTarget = AnnouncementEditTarget(announcement); isEditing = true }
                                    } else null,
                                    onTogglePublish = if (state.isAdmin) {
                                        { viewModel.togglePublish(announcement) }
                                    } else null,
                                    onDeleteRequest = if (state.isAdmin) {
                                        { deleteTarget = announcement }
                                    } else null,
                                    onShare = {
                                        if (shareChurchState.publicUrl.isBlank()) {
                                            Toast.makeText(context, "Endereço público do site não configurado.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val message = buildAnnouncementShareMessage(
                                                title = announcement.title,
                                                description = announcement.description,
                                                churchName = shareChurchState.churchName,
                                                churchCode = shareChurchState.churchCode,
                                                publicUrl = shareChurchState.publicUrl
                                            )
                                            if (announcement.mediaType == com.escalachurch.app.domain.model.MediaType.IMAGE) {
                                                Toast.makeText(context, "Preparando compartilhamento...", Toast.LENGTH_SHORT).show()
                                            }
                                            shareScope.launch { AnnouncementShareUseCase.share(context, announcement, message) }
                                        }
                                    },
                                    activePlayingId = currentlyPlayingId,
                                    onRequestPlay = { currentlyPlayingId = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Excluir anúncio",
            message = "Tem certeza que deseja excluir \"${target.title}\"?",
            onConfirm = { viewModel.delete(target); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

private data class AnnouncementEditTarget(val item: Announcement?)
