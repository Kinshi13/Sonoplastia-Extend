package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.ScaleItem
import com.escalachurch.app.ui.theme.CardShape
import com.escalachurch.app.ui.theme.SpecialGold

/** Flash-card representation of a single [ScaleItem], used on the Início screen. */
@Composable
fun ScaleCard(
    scale: ScaleItem,
    modifier: Modifier = Modifier
) {
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
                Text("Próxima Escala", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                if (scale.isSpecialEvent) {
                    SpecialBadge()
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

            RoleRow(Icons.Filled.Groups, "Recepção", scale.receptionPerson)
            RoleRow(Icons.Filled.Speaker, "Sonoplastia", scale.soundPerson)
            RoleRow(Icons.Filled.RecordVoiceOver, "Pregação", scale.preachingPerson)
            RoleRow(Icons.Filled.Church, "Regência", scale.conductingPerson)
            RoleRow(Icons.Filled.MusicNote, "Mensagem musical", scale.musicalMessagePerson)

            if (scale.notes.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("Observações", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(scale.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RoleRow(icon: ImageVector, label: String, person: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
