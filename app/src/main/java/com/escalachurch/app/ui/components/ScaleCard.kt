package com.escalachurch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.domain.model.SourceType
import com.escalachurch.app.domain.model.UserClass
import com.escalachurch.app.export.ExportFormat
import com.escalachurch.app.export.ScaleExporter
import com.escalachurch.app.ui.theme.CardShape
import com.escalachurch.app.ui.theme.SpecialGold

/**
 * Flash-card representation of a single [ScaleItem], used on the Início screen.
 *
 * [highlightClasses] softly highlights the role rows that match the viewer's selected
 * [UserClass]es (e.g. a Sonoplasta sees the Sonoplastia row stand out), and [recentlyUpdated]
 * shows a small "atualizado" indicator when this scale has an unseen change-log entry.
 */
@Composable
fun ScaleCard(
    scale: ScaleItem,
    modifier: Modifier = Modifier,
    highlightClasses: Set<UserClass> = emptySet(),
    recentlyUpdated: Boolean = false,
    showExportAction: Boolean = true
) {
    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Próxima Escala", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    if (recentlyUpdated) {
                        Spacer(Modifier.width(6.dp))
                        UpdatedDot()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(scale.sourceType)
                    if (scale.isSpecialEvent) SpecialBadge()
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(scale.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

            Spacer(Modifier.height(4.dp))

            Text(
                "${scale.date.dayOfWeekLabel()} · ${scale.date.toDisplayString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                buildString {
                    append(scale.startTime.toDisplayString())
                    scale.endTime?.let { append(" – ${it.toDisplayString()}") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            RoleRow(Icons.Filled.Groups, "Recepção", scale.receptionPerson, UserClass.RECEPCIONISTA in highlightClasses)
            RoleRow(Icons.Filled.Speaker, "Sonoplastia", scale.soundPerson, UserClass.SONOPLASTA in highlightClasses)
            RoleRow(Icons.Filled.RecordVoiceOver, "Pregação", scale.preachingPerson, UserClass.PREGADOR in highlightClasses)
            RoleRow(Icons.Filled.Church, "Regência", scale.conductingPerson, UserClass.REGENTE in highlightClasses)
            RoleRow(Icons.Filled.MusicNote, "Mensagem musical", scale.musicalMessagePerson, UserClass.CANTOR in highlightClasses)

            if (scale.notes.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("Observações", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(scale.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (showExportAction) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Exportar escala", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exportar escala") },
            text = { Text("Escolha o formato para compartilhar esta escala.") },
            confirmButton = {
                TextButton(onClick = {
                    ScaleExporter.share(context, scale, ExportFormat.PDF)
                    showExportDialog = false
                }) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    ScaleExporter.share(context, scale, ExportFormat.JPEG)
                    showExportDialog = false
                }) { Text("JPEG") }
            }
        )
    }
}

@Composable
private fun UpdatedDot() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .height(6.dp)
                .width(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
        )
        Spacer(Modifier.width(4.dp))
        Text("Atualizado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun RoleRow(icon: ImageVector, label: String, person: String, highlighted: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .let {
                if (highlighted) {
                    it
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                } else it
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                person.ifBlank { "Não definido" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SpecialBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .padding(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Surface(
            color = SpecialGold.copy(alpha = 0.15f),
            shape = RoundedCornerShape(50),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SpecialGold, modifier = Modifier.height(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Especial", style = MaterialTheme.typography.labelSmall, color = SpecialGold)
            }
        }
    }
}
