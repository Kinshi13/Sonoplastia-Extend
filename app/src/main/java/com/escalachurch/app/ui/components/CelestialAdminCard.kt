package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** One entry in a [CelestialAdminCard]'s overflow menu - [isDestructive] just tints the row/icon
 *  red (error), the caller still owns whether a confirmation dialog runs before [onClick]. */
data class CelestialAdminCardAction(
    val label: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Fase 11.9B Bloco 15 - the shared "admin list item" shape: [CelestialFrame] + a header row
 * (optional leading icon, title/subtitle, optional status chip, Editar as the one always-visible
 * action, everything else - Duplicar, Exportar, Excluir - tucked behind a discrete overflow menu).
 * "Editar continua sendo ação principal... Nunca manter Excluir visível" (Bloco 15) - Excluir only
 * ever reaches [CelestialAdminCardAction.onClick] through that menu, never as its own icon button.
 *
 * [content] is the rest of the card body (role chips, timeline, notes, etc.) - this component only
 * owns the header/menu chrome, not what an Escala/Doxologia/Anúncio card actually shows.
 */
@Composable
fun CelestialAdminCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tone: CelestialTone = CelestialTone.ADMIN,
    leadingIcon: (@Composable () -> Unit)? = null,
    status: (@Composable () -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    overflowActions: List<CelestialAdminCardAction> = emptyList(),
    content: @Composable ColumnScope.() -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CelestialFrame(modifier = modifier.fillMaxWidth(), tone = tone, cornerRadius = 22.dp) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                leadingIcon?.invoke()
                Column(modifier = Modifier.weight(1f).padding(start = if (leadingIcon != null) 10.dp else 0.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                status?.invoke()
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                }
                if (overflowActions.isNotEmpty()) {
                    Column {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Mais ações")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            overflowActions.forEach { action ->
                                val tint = if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                DropdownMenuItem(
                                    text = { Text(action.label, color = tint) },
                                    leadingIcon = { Icon(action.icon, contentDescription = null, tint = tint) },
                                    onClick = { menuExpanded = false; action.onClick() }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}
