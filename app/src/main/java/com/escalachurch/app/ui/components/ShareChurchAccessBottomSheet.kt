package com.escalachurch.app.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.share.ChurchAccessAnalytics
import com.escalachurch.app.share.ChurchShareImageUseCase
import com.escalachurch.app.share.ShareChurchAccessAction

/**
 * Fase 11.10 (correção) - "Compartilhar acesso da igreja". FREE-available (no FeatureGate/
 * EntitlementService check anywhere in this file). [nextScale] is optional - Home and Escala
 * Geral pass their already-loaded upcoming scale; Stella Core and the admin Configurações entry
 * point open this with `null`, and the sheet degrades gracefully (ChurchShareImageUseCase falls
 * back to text-only).
 *
 * Every value shown/shared/copied here comes from [ShareChurchAccessUiState] - this composable
 * never builds a link or a message itself (that was the original bug: the message used to be
 * assembled inline in this file from separate pieces instead of one ViewModel-owned state).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareChurchAccessBottomSheet(
    nextScale: ScaleItem?,
    onDismiss: () -> Unit
) {
    val viewModel = appViewModel { container -> ShareChurchAccessViewModel(container.activeChurchManager) }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Compartilhar acesso da igreja", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

            if (state.isLoading) {
                Text("Carregando...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            state.errorMessage?.let { error ->
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                // Bloco 11 (correção) - "permitir compartilhar código e texto, caso possível" even
                // when the public URL couldn't be built (missing domain, not missing church data).
                if (state.churchCode.isBlank()) return@Column
            }

            ChurchAccessPreviewCard(
                churchName = state.churchName,
                churchSlug = state.churchCode,
                link = state.publicUrl.ifBlank { "-" },
                nextScale = nextScale
            )

            PrimaryButton(
                text = "Compartilhar",
                enabled = state.shareMessage.isNotBlank() || state.churchCode.isNotBlank(),
                onClick = {
                    // Fase 11.10 (correção) - always the ViewModel's shareMessage; when it's blank
                    // (no publicUrl yet) fall back to a minimal text with just the code, never a
                    // silently-fixed/default message.
                    val text = state.shareMessage.ifBlank { "Código da igreja: ${state.churchCode}" }
                    ChurchShareImageUseCase.share(context, nextScale, text)
                    ChurchAccessAnalytics.logShareEvent(state.churchSlug, ShareChurchAccessAction.SHARE_SHEET)
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    enabled = state.publicUrl.startsWith("http://") || state.publicUrl.startsWith("https://"),
                    onClick = {
                        clipboard.setText(AnnotatedString(state.publicUrl))
                        Toast.makeText(context, "Link da igreja copiado", Toast.LENGTH_SHORT).show()
                        ChurchAccessAnalytics.logShareEvent(state.churchSlug, ShareChurchAccessAction.COPY_LINK)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar link")
                }
                OutlinedButton(
                    enabled = state.churchCode.isNotBlank(),
                    onClick = {
                        clipboard.setText(AnnotatedString(state.churchCode))
                        Toast.makeText(context, "Código da igreja copiado", Toast.LENGTH_SHORT).show()
                        ChurchAccessAnalytics.logShareEvent(state.churchSlug, ShareChurchAccessAction.COPY_CODE)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar código")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    enabled = state.publicUrl.startsWith("http://") || state.publicUrl.startsWith("https://"),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.publicUrl)))
                        ChurchAccessAnalytics.logShareEvent(state.churchSlug, ShareChurchAccessAction.OPEN_SITE)
                    }
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Abrir site")
                }
                // Fase 11.10 - placeholder only, per the phase's own instruction: "preparar
                // arquitetura, não precisa implementar geração ainda". See GenerateChurchQrUseCase.
                TextButton(onClick = {}, enabled = false) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gerar QR Code (em breve)")
                }
            }
        }
    }
}
