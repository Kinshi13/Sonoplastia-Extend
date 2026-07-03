package com.escalachurch.app.ui.screens.announcements

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.data.repository.UploadedMedia
import com.escalachurch.app.domain.model.Announcement
import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.ui.components.AppTextField
import com.escalachurch.app.ui.components.ConfirmDialog
import com.escalachurch.app.ui.components.DatePickerField
import com.escalachurch.app.ui.components.PrimaryButton
import com.escalachurch.app.ui.components.SecondaryButton
import com.escalachurch.app.ui.components.UserClassChips
import kotlinx.coroutines.launch

/**
 * Create/edit form for an [Announcement]. All announcements are official (admin-only per spec),
 * so [isAdmin] is a hard gate - a non-admin caller is bounced straight back, even if a future
 * navigation path reaches this screen directly (today AnnouncementsScreen already only offers
 * this screen to admins, but this keeps the guarantee at the source, not just at the call site).
 *
 * Picked files are uploaded to Supabase Storage right away (see [onUpload]) so [mediaUrl] always
 * ends up holding a real public URL.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementEditScreen(
    existing: Announcement?,
    isAdmin: Boolean,
    onUpload: suspend (Uri) -> UploadedMedia,
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
    var mediaUrl by remember { mutableStateOf(existing?.mediaUrl) }
    var mediaFileName by remember { mutableStateOf(existing?.mediaFileName) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var affectedClasses by remember { mutableStateOf(existing?.affectedClasses ?: emptySet()) }
    var relatedEventDate by remember { mutableStateOf(existing?.relatedEventDate) }
    var isPinned by remember { mutableStateOf(existing?.isPinned ?: false) }

    var showTitleError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun pickAndUpload(uri: Uri?) {
        if (uri == null) return
        isUploading = true
        uploadError = null
        scope.launch {
            runCatching { onUpload(uri) }
                .onSuccess { uploaded ->
                    mediaType = uploaded.type
                    mediaUrl = uploaded.url
                    mediaFileName = uploaded.fileName
                }
                .onFailure { error ->
                    uploadError = "Falha ao enviar o arquivo: ${error.message ?: error::class.simpleName}"
                }
            isUploading = false
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { pickAndUpload(it) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { pickAndUpload(it) }

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
                "Imagem ou vídeo curto para o feed. PPT/PDF e outros arquivos vão na tela de Sonoplastia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Imagem", enabled = !isUploading, onClick = { imagePicker.launch("image/*") })
                SecondaryButton(text = "Vídeo", enabled = !isUploading, onClick = { videoPicker.launch("video/*") })
            }

            if (isUploading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Enviando arquivo...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (uploadError != null) {
                Text(uploadError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (!mediaUrl.isNullOrBlank() && !isUploading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (mediaType) {
                            MediaType.VIDEO -> "Vídeo enviado"
                            MediaType.DOCUMENT -> "Documento enviado: ${mediaFileName ?: ""}"
                            else -> "Imagem enviada (proporção 4:3)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(text = "Remover", onClick = { mediaType = MediaType.NONE; mediaUrl = null; mediaFileName = null })
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
                enabled = !isUploading,
                onClick = {
                    if (title.isBlank()) { showTitleError = true; return@PrimaryButton }
                    onSave(
                        Announcement(
                            id = existing?.id ?: "",
                            title = title,
                            description = description,
                            mediaType = mediaType,
                            mediaUrl = mediaUrl,
                            mediaFileName = mediaFileName,
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
