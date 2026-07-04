package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.DoxologyItem
import com.escalachurch.app.ui.theme.CardShape

/** Flash-card representation of a [DoxologyItem] (order of service) for the Doxologia screen. */
@Composable
fun DoxologyCard(
    doxology: DoxologyItem,
    modifier: Modifier = Modifier,
    isLive: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isLive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Doxologia", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                if (isLive) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text(
                            "Agora",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(doxology.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "${doxology.date.dayOfWeekLabel()} · ${doxology.date.toDisplayString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                doxology.startTime.toDisplayString() + (doxology.endTime?.let { " - ${it.toDisplayString()}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            doxology.programOrder.sortedBy { it.order }.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                            Text(
                                "${step.order}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(step.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        if (step.responsiblePerson.isNotBlank()) {
                            Text(
                                step.responsiblePerson,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (index < doxology.programOrder.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
            }

            if (doxology.notes.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(doxology.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
