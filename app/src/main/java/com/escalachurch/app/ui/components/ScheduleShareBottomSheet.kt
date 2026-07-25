package com.escalachurch.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.export.ScheduleShareCardRenderer
import com.escalachurch.app.share.ScheduleShareUseCase
import com.escalachurch.app.share.buildScheduleShareMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android replica of the web's Export Studio "Compartilhar escala" flow. [canExport] mirrors the
 * exact same `FeatureKey.EXPORT` gate [com.escalachurch.app.export.ScaleExporter]'s PDF/JPEG
 * export already uses (see GeneralScaleScreen) - no new entitlement decision here, and the
 * FREE-plan text/link/code share below is never gated by it, per "não bloquear o compartilhamento
 * básico no FREE".
 *
 * Every value shown/shared/copied comes from [ShareChurchAccessUiState] (reused, not duplicated -
 * "não duplicar builder do link público") plus [nextScale] and one locally-generated [File] - the
 * preview image, the download, and the share button all read that same file, so they can never
 * show different data from each other.
 *
 * Mobile-safe by construction (the exact class of bug just fixed on the web version): the whole
 * sheet is one scrollable Column with explicit [Modifier.navigationBarsPadding]/[Modifier.imePadding],
 * so its own action buttons can never end up hidden behind the system nav bar or a keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleShareBottomSheet(
    nextScale: ScaleItem?,
    canExport: Boolean,
    isFreePlan: Boolean,
    onSeePlans: () -> Unit,
    onDismiss: () -> Unit
) {
    val viewModel = appViewModel { container -> ShareChurchAccessViewModel(container.activeChurchManager) }
    val churchState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var generateError by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(2500)
            feedback = null
        }
    }

    val dateLabel = nextScale?.let { "${it.date.dayOfWeekLabel()} · ${it.date.toDisplayString()}" }.orEmpty()
    val timeLabel = nextScale?.let { scale ->
        val start = scale.startTime.toDisplayString()
        scale.endTime?.let { "$start - ${it.toDisplayString()}" } ?: start
    }.orEmpty()

    val shareText = buildScheduleShareMessage(
        churchName = churchState.churchName,
        churchCode = churchState.churchCode,
        publicUrl = churchState.publicUrl.ifBlank { "-" },
        scheduleTitle = nextScale?.title.orEmpty(),
        dateLabel = dateLabel,
        timeLabel = timeLabel
    )

    fun generateCard() {
        val scale = nextScale ?: return
        isGenerating = true
        generateError = null
        scope.launch(Dispatchers.Default) {
            try {
                val file = ScheduleShareCardRenderer.writePng(
                    context, scale, churchState.churchName, churchState.churchCode, churchState.publicUrl, isFreePlan
                )
                val bitmap = BitmapFactory.decodeFile(file.path)
                withContext(Dispatchers.Main) {
                    imageFile = file
                    previewBitmap = bitmap
                    isGenerating = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    generateError = "Não foi possível preparar a imagem."
                    isGenerating = false
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Compartilhar escala", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

            if (churchState.isLoading) {
                Text("Carregando...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            churchState.errorMessage?.let { error ->
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            if (nextScale == null) {
                Text("Nenhuma escala futura para compartilhar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(nextScale.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("$dateLabel · $timeLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    shareText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            PrimaryButton(
                text = "Compartilhar",
                enabled = shareText.isNotBlank(),
                onClick = { ScheduleShareUseCase.share(context, imageFile, shareText) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(shareText))
                        feedback = "Texto copiado."
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar texto")
                }
                OutlinedButton(
                    enabled = churchState.publicUrl.isNotBlank(),
                    onClick = {
                        clipboard.setText(AnnotatedString(churchState.publicUrl))
                        feedback = "Link copiado."
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar link")
                }
            }
            OutlinedButton(
                enabled = churchState.churchCode.isNotBlank(),
                onClick = {
                    clipboard.setText(AnnotatedString(churchState.churchCode))
                    feedback = "Código copiado."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copiar código")
            }

            HorizontalDivider()

            Text("Card visual da escala", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

            when {
                !canExport -> LockedCardVisualNotice(onSeePlans = onSeePlans)
                nextScale == null -> Text(
                    "Nenhuma escala futura para gerar o card.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                previewBitmap == null -> {
                    PrimaryButton(text = if (isGenerating) "Gerando..." else "Gerar card", enabled = !isGenerating, onClick = ::generateCard)
                    generateError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
                else -> {
                    androidx.compose.foundation.Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Prévia do card da escala: ${nextScale.title}",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                    )
                    OutlinedButton(
                        onClick = {
                            val bitmap = previewBitmap ?: return@OutlinedButton
                            val filename = "escala-${churchState.churchCode.ifBlank { "igreja" }}-${nextScale.date}.png"
                            val ok = GallerySaver.savePngToGallery(context, bitmap, filename)
                            feedback = if (ok) "Imagem salva na galeria." else "Não foi possível salvar a imagem."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.height(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Baixar PNG")
                    }
                }
            }

            feedback?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LockedCardVisualNotice(onSeePlans: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Card visual faz parte dos planos pagos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Gerar a imagem estilizada da escala é um recurso Premium. O texto e o link acima continuam disponíveis no plano Free.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onSeePlans) { Text("Ver planos") }
        }
    }
}
