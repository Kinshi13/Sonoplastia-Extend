package com.escalachurch.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Fase 11.9B Bloco 16 - one entry shared by every admin menu ([CelestialOverflowMenu],
 * [CelestialContextMenu], and [CelestialAdminCard]'s own overflow before this bloco). Editar/
 * Duplicar/Excluir/Exportar/Visualizar público only ever show up here "conforme disponibilidade" -
 * callers just omit whichever action doesn't apply to that entity, nothing renders a disabled/dead
 * item. [isDestructive] only tints the row red; the caller still owns whether a confirmation
 * dialog runs before [onClick] (Bloco 16: "Excluir sempre exige confirmação").
 */
data class CelestialMenuAction(
    val label: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun CelestialMenuItems(actions: List<CelestialMenuAction>, onItemClick: (CelestialMenuAction) -> Unit) {
    actions.forEach { action ->
        val tint = if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        DropdownMenuItem(
            text = { Text(action.label, color = tint) },
            leadingIcon = { Icon(action.icon, contentDescription = null, tint = tint) },
            onClick = { onItemClick(action) }
        )
    }
}

/** The "..." trigger + menu used on admin cards - a thin, focused wrapper around [DropdownMenu]
 *  (itself already backed by PopupWindow, so open/close is instant - no extra animation layered
 *  on top here, matching Bloco 16's "menus devem abrir rapidamente, sem travamentos"). */
@Composable
fun CelestialOverflowMenu(actions: List<CelestialMenuAction>, modifier: Modifier = Modifier) {
    if (actions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Mais ações")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CelestialMenuItems(actions) { expanded = false; it.onClick() }
        }
    }
}

/**
 * Long-press variant for cards that don't have (or don't want) a visible overflow button -
 * Doxologia/Anúncios' list cards today have no admin affordance at all outside the full edit
 * screen, so this is how they get quick actions without adding a permanent icon to every card
 * (Bloco 16: "menus contextuais discretos"). Wraps [content]; a long-press anywhere on it opens
 * the same [CelestialMenuAction] list as [CelestialOverflowMenu]. No-ops (no long-press handler at
 * all) when [actions] is empty, so a non-admin viewer sees zero behavior change.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CelestialContextMenu(
    actions: List<CelestialMenuAction>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (actions.isEmpty()) {
        Box(modifier = modifier) { content() }
        return
    }
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
            onLongClick = { expanded = true }
        )
    ) {
        content()
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CelestialMenuItems(actions) { expanded = false; it.onClick() }
        }
    }
}

/** A labeled, closed-choice dropdown (e.g. "Exportar como" -> PDF/JPEG) - distinct from the two
 *  action menus above, this picks a value rather than firing a command. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CelestialDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
