package com.escalachurch.app.ui.screens.worship

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ArrowBack
import coil.compose.AsyncImage
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.domain.model.WorshipSong
import com.escalachurch.app.ui.components.ConfirmDialog
import com.escalachurch.app.ui.components.EmptyState

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WorshipScreen(onBack: () -> Unit = {}) {
    val viewModel = appViewModel { container -> WorshipViewModel(container.worshipSongRepository, container.adminSession) }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var editing: WorshipSong? by remember { mutableStateOf(null) }
    var creatingNew by remember { mutableStateOf(false) }
    var pendingDelete: WorshipSong? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Música e Louvor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.isAdmin) {
                FloatingActionButton(onClick = { creatingNew = true }) { Icon(Icons.Default.Add, contentDescription = "Adicionar música") }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.padding(32.dp)) }
            state.hasLoadError -> EmptyState(
                icon = Icons.Default.MusicNote,
                title = "Não foi possível carregar",
                message = "Verifique sua conexão e tente novamente.",
                modifier = Modifier.padding(padding)
            )
            state.recommendation == null && state.currentProgram.isEmpty() && state.nextProgram.isEmpty() -> EmptyState(
                icon = Icons.Default.MusicNote,
                title = "Nenhuma música cadastrada",
                message = if (state.isAdmin) "Toque em + para adicionar a primeira música." else "Em breve, novidades de Música e Louvor.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.recommendation?.let { rec ->
                    item {
                        RecommendationCard(
                            song = rec,
                            isAdmin = state.isAdmin,
                            onPlay = { openYoutube(context, rec.youtubeUrl) },
                            onShare = { shareSong(context, rec) },
                            onEdit = { editing = rec }
                        )
                    }
                }
                if (state.currentProgram.isNotEmpty()) {
                    item { SectionHeader("Programação atual") }
                    items(state.currentProgram, key = { it.id }) { song ->
                        WorshipSongCard(
                            song = song,
                            isAdmin = state.isAdmin,
                            onPlay = { openYoutube(context, song.youtubeUrl) },
                            onShare = { shareSong(context, song) },
                            onEdit = { editing = song },
                            onDelete = { pendingDelete = song },
                            onTogglePublish = { viewModel.setPublished(song, !song.isPublished) }
                        )
                    }
                }
                if (state.nextProgram.isNotEmpty()) {
                    item { SectionHeader("Próxima programação") }
                    items(state.nextProgram, key = { it.id }) { song ->
                        WorshipSongCard(
                            song = song,
                            isAdmin = state.isAdmin,
                            onPlay = { openYoutube(context, song.youtubeUrl) },
                            onShare = { shareSong(context, song) },
                            onEdit = { editing = song },
                            onDelete = { pendingDelete = song },
                            onTogglePublish = { viewModel.setPublished(song, !song.isPublished) }
                        )
                    }
                }
            }
        }
    }

    if (creatingNew) {
        WorshipSongEditDialog(
            existing = null,
            viewModel = viewModel,
            onDismiss = { creatingNew = false }
        )
    }
    editing?.let { song ->
        WorshipSongEditDialog(
            existing = song,
            viewModel = viewModel,
            onDismiss = { editing = null }
        )
    }
    pendingDelete?.let { song ->
        ConfirmDialog(
            title = "Remover música",
            message = "Remover \"${song.title}\" de Música e Louvor?",
            onConfirm = { viewModel.delete(song); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun RecommendationCard(
    song: WorshipSong,
    isAdmin: Boolean,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("Recomendação do dia") })
            if (song.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
                )
            }
            Text(song.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (song.artist.isNotBlank()) Text(song.artist, style = MaterialTheme.typography.bodyMedium)
            song.recommendationMessage?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            SongActionsRow(song = song, isAdmin = isAdmin, onPlay = onPlay, onShare = onShare, onEdit = onEdit, onDelete = null, onTogglePublish = null)
        }
    }
}

@Composable
private fun WorshipSongCard(
    song: WorshipSong,
    isAdmin: Boolean,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePublish: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (song.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (song.artist.isNotBlank()) Text(song.artist, style = MaterialTheme.typography.bodySmall)
                if (song.momentLabel.isNotBlank()) Text(song.momentLabel, style = MaterialTheme.typography.labelMedium)
                if (isAdmin && !song.isPublished) Text("Rascunho", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                SongActionsRow(song = song, isAdmin = isAdmin, onPlay = onPlay, onShare = onShare, onEdit = onEdit, onDelete = onDelete, onTogglePublish = onTogglePublish)
            }
        }
    }
}

@Composable
private fun SongActionsRow(
    song: WorshipSong,
    isAdmin: Boolean,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    onTogglePublish: (() -> Unit)?
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        IconButton(onClick = onPlay, enabled = song.youtubeUrl.isNotBlank()) { Icon(Icons.Default.PlayArrow, contentDescription = "Ouvir") }
        IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "Compartilhar") }
        if (isAdmin) {
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
            onTogglePublish?.let {
                IconButton(onClick = it) {
                    Icon(if (song.isPublished) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Publicar/despublicar")
                }
            }
            onDelete?.let { IconButton(onClick = it) { Icon(Icons.Default.Delete, contentDescription = "Remover") } }
        }
    }
}


/** Bloco A2 - ACTION_VIEW, letting Android offer YouTube/YouTube Music/browser (never an embedded
 *  player in this phase). */
private fun openYoutube(context: android.content.Context, url: String) {
    if (url.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun shareSong(context: android.content.Context, song: WorshipSong) {
    val text = buildString {
        append(song.title)
        if (song.artist.isNotBlank()) append(" - ${song.artist}")
        if (song.youtubeUrl.isNotBlank()) append("\n${song.youtubeUrl}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Compartilhar música")) }
}
