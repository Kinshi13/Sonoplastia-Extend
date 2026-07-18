package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.ScaleItem

/**
 * Fase 11.10 - the "prévia" inside ShareChurchAccessBottomSheet: church name/slug/link, plus a
 * compact hero (mini version of ScaleCard's header, not the full role list) for the next scale
 * when there is one. Same Celestial identity as the rest of the app (CelestialFrame, day
 * constellation glyph) - not a bespoke look for this one feature.
 */
@Composable
fun ChurchAccessPreviewCard(
    churchName: String,
    churchSlug: String,
    link: String,
    nextScale: ScaleItem?,
    modifier: Modifier = Modifier
) {
    CelestialFrame(modifier = modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(churchName.ifBlank { "Igreja" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("Código: ${churchSlug.ifBlank { "-" }.uppercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(link.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

            if (nextScale != null) {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val isDark = MaterialTheme.colorScheme.background.let {
                        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
                    }
                    val kind = nextScale.constellationKind()
                    DayConstellationGlyph(
                        kind = kind,
                        modifier = Modifier.size(28.dp),
                        intensity = ConstellationIntensity.COMPACT,
                        tint = kind.tintColor(isDark),
                        accentColor = kind.accentColor(isDark)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(nextScale.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "${nextScale.date.dayOfWeekLabel()} · ${nextScale.date.toDisplayString()} · ${nextScale.startTime.toDisplayString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${nextScale.assignedRolesByClass().size} função(ões) escalada(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Nenhuma escala futura para mostrar na imagem - o link e o código continuam funcionando normalmente.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
