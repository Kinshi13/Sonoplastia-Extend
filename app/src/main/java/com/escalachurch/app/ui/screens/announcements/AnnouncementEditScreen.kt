package com.escalachurch.app.ui.screens.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.ConfirmDialog
import com.escalachurch.app.ui.components.DatePickerField
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.UserClassChips

/**
 * Create/edit form for an [Announcement]. All announcements are official (admin-only per spec),
 * so [isAdmin] is a hard gate - a non-admin caller is bounced straight back, even if a future
 * navigation path reaches this screen directly (today AnnouncementsScreen already only offers
 * this screen to admins, but this keeps the guarantee at the source, not just at the call site).
 *
 * Media is a pasted link (Google Drive, YouTube, etc.) rather than a direct upload, since Firebase
 * Storage requires the paid Blaze plan - the admin uploads the file to their own free Drive/Photos
 * account and shares the link here instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementEditScreen(
    existing: Announcement?,
    isAdmin: Boolean,
    onSave: (Announcement) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    if (!isAdmin) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
        return
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var mediaType by remember { mutableStateOf(existing?.mediaType ?: MediaType.NONE) }
    var mediaUrl by remember { mutableStateOf(existing?.mediaUrl ?: "") }
    var mediaFileName by remember { mutableStateOf(existing?.mediaFileName ?: "") }
    var affectedClasses by remember { mutableStateOf(existing?.affectedClasses ?: emptySet()) }
    var relatedEventDate by remember { mutableStateOf(existing?.relatedEventDate) }
    var isPinned by remember { mutableStateOf(existing?.isPinned ?: false) }

    var showTitleError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Novo anúncio" else "Editar anúncio") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppTextField(
                value = title,
                onValueChange = { title = it; showTitleError = false },
                label = "Título",
                isError = showTitleError,
                supportingText = if (showTitleError) "Informe um título para o anúncio" else null
            )
            AppTextField(value = description, onValueChange = { description = it }, label = "Texto do anúncio", singleLine = false, minLines = 3)

            Text("Mídia (opcional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "Suba o arquivo (imagem, vídeo, PPT ou PDF) no seu Google Drive, gere o link de compartilhamento e cole abaixo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaTypeOption(label = "Nenhuma", selected = mediaType == MediaType.NONE) { mediaType = MediaType.NONE; mediaUrl = ""; mediaFileName = "" }
                MediaTypeOption(label = "Imagem", selected = mediaType == MediaType.IMAGE) { mediaType = MediaType.IMAGE }
                MediaTypeOption(label = "Vídeo", selected = mediaType == MediaType.VIDEO) { mediaType = MediaType.VIDEO }
                MediaTypeOption(label = "Documento", selected = mediaType == MediaType.DOCUMENT) { mediaType = MediaType.DOCUMENT }
            }

            if (mediaType != MediaType.NONE) {
                AppTextField(
                    value = mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = "Link do arquivo (Google Drive, YouTube, etc.)"
                )
                if (mediaType == MediaType.DOCUMENT) {
                    AppTextField(
                        value = mediaFileName,
                        onValueChange = { mediaFileName = it },
                        label = "Nome do arquivo (opcional, ex: Slides do culto)"
                    )
                }
            }

            Text("Classes afetadas", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            UserClassChips(
                selected = affectedClasses,
                onToggle = { cls -> affectedClasses = if (cls in affectedClasses) affectedClasses - cls else affectedClasses + cls }
            )

            DatePickerField(
                label = "Data do evento (opcional)",
                date = relatedEventDate,
                onDateSelected = { relatedEventDate = it }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                Text("Fixar no topo do feed", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            PrimaryButton(
                text = "Publicar",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (title.isBlank()) { showTitleError = true; return@PrimaryButton }
                    val hasMedia = mediaType != MediaType.NONE && mediaUrl.isNotBlank()
                    onSave(
                        Announcement(
                            id = existing?.id ?: "",
                            title = title,
                            description = description,
                            mediaType = if (hasMedia) mediaType else MediaType.NONE,
                            mediaUrl = if (hasMedia) mediaUrl else null,
                            mediaFileName = if (hasMedia) mediaFileName.ifBlank { null } else null,
                            affectedClasses = affectedClasses,
                            relatedEventDate = relatedEventDate,
                            sourceType = SourceType.OFFICIAL,
                            isPinned = isPinned,
                            publishedAt = existing?.publishedAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            )

            if (onDelete != null) {
                SecondaryButton(text = "Excluir anúncio", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteConfirm = true })
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDialog(
            title = "Excluir anúncio",
            message = "Tem certeza que deseja excluir este anúncio?",
            onConfirm = { showDeleteConfirm = false; onDelete() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaTypeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
