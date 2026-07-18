package com.escalachurch.app.ui.components

import android.content.Intent
import android.net.Uri
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
import com.escalachurch.app.BuildConfig
import com.escalachurch.app.di.appViewModel
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.share.ChurchAccessAnalytics
import com.escalachurch.app.share.ChurchShareImageUseCase
import com.escalachurch.app.share.ShareChurchAccessAction

/**
 * Fase 11.10 - "Compartilhar acesso da igreja". FREE-available (no FeatureGate/EntitlementService
 * check anywhere in this file - the phase is explicit this is a FREE-plan feature, unlike
 * ScaleExporter's PDF/JPEG export which stays gated). [nextScale] is optional - Home and Escala
 * Geral pass their already-loaded upcoming scale; Stella Core and the admin Configurações entry
 * point open this with `null` since they don't have that data at hand, and the sheet degrades
 * gracefully (see ChurchAccessPreviewCard's "sem imagem" branch and ChurchShareImageUseCase's
 * text-only fallback) rather than fetching a second copy of it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareChurchAccessBottomSheet(
    nextScale: ScaleItem?,
    onDismiss: () -> Unit
) {
    val viewModel = appViewModel { container ->
        ShareChurchAccessViewModel(container.activeChurchManager, BuildConfig.SITE_URL)
    }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val nextScaleSummary = nextScale?.let {
        "${it.title} · ${it.date.dayOfWeekLabel()} ${it.date.toDisplayString()} · ${it.startTime.toDisplayString()}"
    }
    val message = viewModel.buildMessage(nextScaleSummary)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Compartilhar acesso da igreja", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

            if (!state.isReady) {
                Text(
                    "Não foi possível carregar os dados da igreja ativa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            ChurchAccessPreviewCard(
                churchName = state.churchName,
                churchSlug = state.churchSlug,
                link = state.link,
                nextScale = nextScale
            )

            PrimaryButton(
                text = "Compartilhar",
                onClick = {
                    ChurchShareImageUseCase.share(context, nextScale, message)
                    ChurchAccessAnalytics.logShareEvent(state.churchSlug, ShareChurchAccessAction.SHARE_SHEET)
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(state.link))
                        ChurchAccessAnalytics.logShareEvent(state.churchSlug, ShareChurchAccessAction.COPY_LINK)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar link")
                }
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(state.churchSlug.uppercase()))
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
                    onClick = {
                        if (state.link.isNotBlank()) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.link)))
                            ChurchAccessAnalytics.logShareEvent(state.churchSlug, ShareChurchAccessAction.OPEN_SITE)
                        }
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
