package com.escalachurch.app.ui.screens.sonoplastia

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.escalachurch.app.domain.model.MediaType
import com.escalachurch.app.domain.model.SharedFile
import com.escalachurch.app.ui.components.ConfirmDialog
import com.escalachurch.app.ui.components.EmptyState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Remote file sharing for the sound/media team: upload a PPT/PDF/photo/video here from the phone
 * and open or download it from any other device (including a browser on a PC) - no Bluetooth/Wi-Fi
 * pairing needed, since files live in Supabase Storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SonoplastiaScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { container ->
        SonoplastiaViewModel(container.sonoplastiaFileRepository, container.adminSession)
    }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var fileToDelete by remember { mutableStateOf<SharedFile?>(null) }

    fun pickAndUpload(uri: android.net.Uri?) {
        if (uri == null) return
        isUploading = true
        uploadError = null
        scope.launch {
            runCatching { viewModel.upload(context, uri) }
                .onFailure { error -> uploadError = "Falha ao enviar o arquivo: ${error.message ?: error::class.simpleName}" }
            isUploading = false
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { pickAndUpload(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sonoplastia") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        },
        floatingActionButton = {
            if (state.isAdmin) {
                FloatingActionButton(
                    onClick = { filePicker.launch("*/*") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Filled.Add, contentDescription = "Enviar arquivo") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(top = 12.dp, start = 20.dp, end = 20.dp)) {
            Text(
                "Envie apresentações, PDFs, fotos e vídeos daqui para acessar de qualquer dispositivo - inclusive pelo navegador no computador.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (isUploading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Enviando arquivo...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (uploadError != null) {
                Text(uploadError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            if (state.files.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Computer,
                    title = "Nenhum arquivo compartilhado ainda.",
                    message = if (state.isAdmin) {
                        "Toque no + para enviar o primeiro arquivo."
                    } else {
                        "Quando um administrador enviar um arquivo, ele aparecerá aqui."
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(state.files, key = { it.id }) { file ->
                        SharedFileRow(
                            file = file,
                            canDelete = state.isAdmin,
                            onOpen = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(file.url))
                                runCatching { context.startActivity(intent) }
                            },
                            onDelete = { fileToDelete = file }
                        )
                    }
                }
            }
        }
    }

    fileToDelete?.let { target ->
        ConfirmDialog(
            title = "Excluir arquivo",
            message = "Remover \"${target.fileName}\" para todos os dispositivos?",
            onConfirm = { viewModel.delete(target); fileToDelete = null },
            onDismiss = { fileToDelete = null }
        )
    }
}

@Composable
private fun SharedFileRow(
    file: SharedFile,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(file.mediaType.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.fileName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(formatUploadedAt(file.uploadedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Close, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun MediaType.icon() = when (this) {
    MediaType.IMAGE -> Icons.Filled.Image
    MediaType.VIDEO -> Icons.Filled.Videocam
    else -> Icons.Filled.Description
}

private fun formatUploadedAt(epochMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(epochMillis))
