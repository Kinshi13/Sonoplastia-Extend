package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.escalachurch.app.domain.model.SourceType

/** Small, discreet "Oficial" (blue) / "Pessoal" (gray) badge distinguishing data origin. */
@Composable
fun SourceBadge(sourceType: SourceType, modifier: Modifier = Modifier) {
    val (label, containerColor, contentColor) = when (sourceType) {
        SourceType.OFFICIAL -> Triple(
            "Oficial",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        SourceType.PERSONAL -> Triple(
            "Pessoal",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Surface(shape = RoundedCornerShape(50), color = containerColor, modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
